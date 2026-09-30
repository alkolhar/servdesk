package dev.alkolhar.servdesk.ticket.incident;

import static org.assertj.core.api.Assertions.assertThat;

import dev.alkolhar.servdesk.TestcontainersConfiguration;
import dev.alkolhar.servdesk.setup.SetupRequest;
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

/**
 * Covers the one behavior genuinely specific to {@link Incident} rather than
 * shared across every ticket subtype: the optional many-to-one link to the
 * {@code Problem} tracking its root cause. Everything else about Incident CRUD
 * is already covered by {@link IncidentControllerTest} via the shared abstract
 * base.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IncidentRelatedProblemTest {

	@Autowired
	private TestRestTemplate restTemplate;

	private Number requesterId;

	@BeforeAll
	void bootstrapFixtures() {
		restTemplate.postForEntity("/api/setup",
				new SetupRequest("Administrator", "admin@example.com", null, "admin", "admin-password"), String.class);
		Map<String, Object> customerRequest = Map.of("role", "CUSTOMER", "name", "Carla Customer", "email",
				"carla@example.com");
		requesterId = (Number) asAdmin().postForEntity("/api/persons", customerRequest, Map.class).getBody().get("id");
	}

	private TestRestTemplate asAdmin() {
		return restTemplate.withBasicAuth("admin", "admin-password");
	}

	@Test
	void linksAnIncidentToTheProblemTrackingItsRootCause() {
		Number problemId = (Number) asAdmin().postForEntity("/api/problems",
				Map.of("subject", "Recurring outage", "requesterId", requesterId), Map.class).getBody().get("id");

		ResponseEntity<Map> created = asAdmin().postForEntity("/api/incidents",
				Map.of("subject", "Yet another outage", "requesterId", requesterId, "relatedProblemId", problemId),
				Map.class);

		assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(((Number) created.getBody().get("relatedProblemId")).longValue()).isEqualTo(problemId.longValue());
	}

	@Test
	void incidentsWithNoIdentifiedProblemLeaveTheLinkNull() {
		ResponseEntity<Map> created = asAdmin().postForEntity("/api/incidents",
				Map.of("subject", "Standalone incident", "requesterId", requesterId), Map.class);

		assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(created.getBody().get("relatedProblemId")).isNull();
	}

	@Test
	void updateCanChangeWhichProblemAnIncidentIsLinkedTo() {
		Number firstProblemId = (Number) asAdmin().postForEntity("/api/problems",
				Map.of("subject", "First root cause", "requesterId", requesterId), Map.class).getBody().get("id");
		Number secondProblemId = (Number) asAdmin().postForEntity("/api/problems",
				Map.of("subject", "Second root cause", "requesterId", requesterId), Map.class).getBody().get("id");
		Number incidentId = (Number) asAdmin().postForEntity("/api/incidents",
				Map.of("subject", "Relinked incident", "requesterId", requesterId, "relatedProblemId", firstProblemId),
				Map.class).getBody().get("id");

		Map<String, Object> updateBody = Map.of("subject", "Relinked incident", "requesterId", requesterId,
				"relatedProblemId", secondProblemId);
		ResponseEntity<Map> updated = asAdmin().exchange("/api/incidents/" + incidentId, HttpMethod.PUT,
				new HttpEntity<>(updateBody), Map.class);

		assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(((Number) updated.getBody().get("relatedProblemId")).longValue())
				.isEqualTo(secondProblemId.longValue());
	}

	private long ticketCount() {
		ResponseEntity<Map> tickets = asAdmin().getForEntity("/api/tickets", Map.class);
		assertThat(tickets.getStatusCode()).isEqualTo(HttpStatus.OK);
		return ((Number) ((Map<?, ?>) tickets.getBody().get("page")).get("totalElements")).longValue();
	}

	/**
	 * Issue #127: the shared Ticket row and the Incident row are one unit. A
	 * related problem that doesn't exist fails the Incident insert; the Ticket
	 * written just before it must go too, or /api/tickets answers 500 for every
	 * page that includes the orphan.
	 */
	@Test
	void aFailedCreateLeavesNoOrphanTicketBehind() {
		long before = ticketCount();

		ResponseEntity<String> created = asAdmin().postForEntity("/api/incidents",
				Map.of("subject", "Points at nothing", "requesterId", requesterId, "relatedProblemId", 999_999),
				String.class);

		assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(ticketCount()).isEqualTo(before);
	}

	/**
	 * The same for an update: the shared fields don't change if the Incident's own
	 * write fails.
	 */
	@Test
	void aFailedUpdateLeavesTheSharedFieldsUntouched() {
		Number id = (Number) asAdmin().postForEntity("/api/incidents",
				Map.of("subject", "Original subject", "requesterId", requesterId), Map.class).getBody().get("id");

		ResponseEntity<String> updated = asAdmin().exchange("/api/incidents/" + id, HttpMethod.PUT,
				new HttpEntity<>(
						Map.of("subject", "Changed subject", "requesterId", requesterId, "relatedProblemId", 999_999)),
				String.class);

		assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		Map<?, ?> incident = asAdmin().getForEntity("/api/incidents/" + id, Map.class).getBody();
		assertThat(incident.get("subject")).isEqualTo("Original subject");
	}
}
