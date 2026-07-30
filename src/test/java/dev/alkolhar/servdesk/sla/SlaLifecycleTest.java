package dev.alkolhar.servdesk.sla;

import static org.assertj.core.api.Assertions.assertThat;

import dev.alkolhar.servdesk.TestcontainersConfiguration;
import dev.alkolhar.servdesk.setup.SetupRequest;
import dev.alkolhar.servdesk.ticket.Ticket;
import dev.alkolhar.servdesk.ticket.TicketRepository;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/**
 * End-to-end SLA behavior through the real stack: policy administration,
 * deadline stamping on create, first-response recording via the comment
 * endpoint, and the breach scanner's stamping/eventing/idempotence — the
 * scanner is exercised by calling {@link SlaScanService} directly rather than
 * waiting for a Quartz tick, since the Quartz shell contains no logic of its
 * own.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@RecordApplicationEvents
class SlaLifecycleTest {

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private SlaScanService slaScanService;

	@Autowired
	private TicketRepository ticketRepository;

	private Number requesterId;
	private Number urgentPriorityId;

	@BeforeAll
	void bootstrapFixtures() {
		restTemplate.postForEntity("/api/setup",
				new SetupRequest("Administrator", "admin@example.com", null, "admin", "admin123"), String.class);
		Map<String, Object> customerRequest = Map.of("role", "CUSTOMER", "name", "Carla Customer", "email",
				"carla@example.com", "username", "carla", "password", "carla12345");
		requesterId = (Number) asAdmin().postForEntity("/api/persons", customerRequest, Map.class).getBody().get("id");
		urgentPriorityId = (Number) asAdmin()
				.postForEntity("/api/priorities", Map.of("name", "Urgent", "sortOrder", 0), Map.class).getBody()
				.get("id");
		ResponseEntity<Map> policy = asAdmin().postForEntity("/api/sla-policies",
				Map.of("priorityId", urgentPriorityId, "responseMinutes", 30, "resolutionMinutes", 240), Map.class);
		assertThat(policy.getStatusCode()).isEqualTo(HttpStatus.CREATED);
	}

	private TestRestTemplate asAdmin() {
		return restTemplate.withBasicAuth("admin", "admin123");
	}

	private Number createUrgentIncident(String subject) {
		Map<String, Object> body = Map.of("subject", subject, "requesterId", requesterId, "priorityId",
				urgentPriorityId);
		ResponseEntity<Map> created = asAdmin().postForEntity("/api/incidents", body, Map.class);
		assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		return (Number) created.getBody().get("id");
	}

	@Test
	void stampsDeadlinesFromThePolicyOnCreate() {
		Number id = createUrgentIncident("Deadline stamping");
		Map<String, Object> fetched = asAdmin().getForEntity("/api/incidents/" + id, Map.class).getBody();
		assertThat(fetched.get("respondBy")).isNotNull();
		assertThat(fetched.get("resolveBy")).isNotNull();

		Instant respondBy = Instant.parse((String) fetched.get("respondBy"));
		Instant resolveBy = Instant.parse((String) fetched.get("resolveBy"));
		assertThat(java.time.Duration.between(respondBy, resolveBy)).isEqualTo(java.time.Duration.ofMinutes(210));
	}

	@Test
	void aNonInternalAgentCommentRecordsTheFirstResponse() {
		Number id = createUrgentIncident("First response");
		asAdmin().postForEntity("/api/tickets/" + id + "/comments",
				Map.of("body", "Looking into it", "internal", false), Map.class);

		Map<String, Object> fetched = asAdmin().getForEntity("/api/incidents/" + id, Map.class).getBody();
		assertThat(fetched.get("firstRespondedAt")).isNotNull();
	}

	@Test
	void anInternalNoteDoesNotCountAsAResponse() {
		Number id = createUrgentIncident("Internal note only");
		asAdmin().postForEntity("/api/tickets/" + id + "/comments",
				Map.of("body", "internal musings", "internal", true), Map.class);

		Map<String, Object> fetched = asAdmin().getForEntity("/api/incidents/" + id, Map.class).getBody();
		assertThat(fetched.get("firstRespondedAt")).isNull();
	}

	@Test
	void theScannerStampsBreachesOnceAndPublishesOneEventEach(ApplicationEvents applicationEvents) {
		Number id = createUrgentIncident("Will breach");
		// time-travel: pretend both deadlines passed an hour ago
		Ticket ticket = ticketRepository.findById(id.longValue()).orElseThrow();
		Instant past = Instant.now().minusSeconds(3600);
		ticket.setRespondBy(past);
		ticket.setResolveBy(past);
		ticketRepository.save(ticket);

		slaScanService.scan(Instant.now());

		Map<String, Object> fetched = asAdmin().getForEntity("/api/incidents/" + id, Map.class).getBody();
		assertThat(fetched.get("responseBreachedAt")).isNotNull();
		assertThat(fetched.get("resolutionBreachedAt")).isNotNull();
		long eventsForTicket = applicationEvents.stream(SlaBreachedEvent.class)
				.filter(event -> event.ticketId().equals(id.longValue())).count();
		assertThat(eventsForTicket).isEqualTo(2);

		// second pass: idempotent, no further events
		slaScanService.scan(Instant.now());
		assertThat(applicationEvents.stream(SlaBreachedEvent.class)
				.filter(event -> event.ticketId().equals(id.longValue())).count()).isEqualTo(2);
	}

	@Test
	void aRespondedTicketDoesNotBreachResponse(ApplicationEvents applicationEvents) {
		Number id = createUrgentIncident("Responded in time");
		asAdmin().postForEntity("/api/tickets/" + id + "/comments", Map.of("body", "On it", "internal", false),
				Map.class);
		Ticket ticket = ticketRepository.findById(id.longValue()).orElseThrow();
		ticket.setRespondBy(Instant.now().minusSeconds(3600));
		ticketRepository.save(ticket);

		slaScanService.scan(Instant.now());

		assertThat(asAdmin().getForEntity("/api/incidents/" + id, Map.class).getBody().get("responseBreachedAt"))
				.isNull();
		assertThat(applicationEvents.stream(SlaBreachedEvent.class)
				.filter(event -> event.ticketId().equals(id.longValue()) && event.type() == SlaBreachType.RESPONSE)
				.count()).isZero();
	}

	@Test
	void pendingPausesTheClock() {
		Number id = createUrgentIncident("Paused while pending");
		Map<String, Object> fetched = asAdmin().getForEntity("/api/incidents/" + id, Map.class).getBody();
		Instant originalRespondBy = Instant.parse((String) fetched.get("respondBy"));

		Map<String, Object> toPending = Map.of("status", "PENDING", "subject", "Paused while pending", "requesterId",
				requesterId, "priorityId", urgentPriorityId);
		asAdmin().exchange("/api/incidents/" + id, HttpMethod.PUT, new HttpEntity<>(toPending), Map.class);

		// backdate the pause start to make the shift observable
		Ticket ticket = ticketRepository.findById(id.longValue()).orElseThrow();
		ticket.setPendingSince(Instant.now().minusSeconds(3600));
		ticketRepository.save(ticket);

		Map<String, Object> toInProgress = Map.of("status", "IN_PROGRESS", "subject", "Paused while pending",
				"requesterId", requesterId, "priorityId", urgentPriorityId);
		Map<String, Object> resumed = asAdmin()
				.exchange("/api/incidents/" + id, HttpMethod.PUT, new HttpEntity<>(toInProgress), Map.class).getBody();

		Instant shiftedRespondBy = Instant.parse((String) resumed.get("respondBy"));
		assertThat(java.time.Duration.between(originalRespondBy, shiftedRespondBy))
				.isGreaterThanOrEqualTo(java.time.Duration.ofMinutes(59));
	}

	@Test
	void rejectsAPolicyWithoutAnyTargetAndADuplicatePolicy() {
		assertThat(asAdmin().postForEntity("/api/sla-policies", Map.of("priorityId", urgentPriorityId), String.class)
				.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(
				asAdmin()
						.postForEntity("/api/sla-policies",
								Map.of("priorityId", urgentPriorityId, "responseMinutes", 5), String.class)
						.getStatusCode())
				.isEqualTo(HttpStatus.CONFLICT);
	}
}
