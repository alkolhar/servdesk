package dev.alkolhar.servdesk.ticket.incident;

import static org.assertj.core.api.Assertions.assertThat;

import dev.alkolhar.servdesk.TestcontainersConfiguration;
import dev.alkolhar.servdesk.setup.SetupRequest;
import dev.alkolhar.servdesk.ticket.Ticket;
import dev.alkolhar.servdesk.ticket.TicketRepository;
import dev.alkolhar.servdesk.ticket.TicketStatus;
import dev.alkolhar.servdesk.ticket.event.TicketStatusChangedEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/**
 * The Incident lifecycle on a real engine (ADR-0004, ADR-0007): one process
 * instance per Incident, started with it, whose stages project onto
 * {@code Ticket.status} through the single entry point. Stages are moved the
 * way the engine moves them — completing the current user task with an
 * {@code outcome} — since the API for that ({@code /actions}) arrives with
 * #111.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@RecordApplicationEvents
class IncidentLifecycleTest {

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private RuntimeService runtimeService;

	@Autowired
	private TaskService taskService;

	@Autowired
	private HistoryService historyService;

	@Autowired
	private TicketRepository ticketRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private ApplicationEvents events;

	private Number requesterId;

	@BeforeAll
	void bootstrapFixtures() {
		restTemplate.postForEntity("/api/setup",
				new SetupRequest("Administrator", "admin@example.com", null, "admin", "admin-password"), String.class);
		requesterId = (Number) asAdmin()
				.postForEntity("/api/persons",
						Map.of("role", "CUSTOMER", "name", "Carla Customer", "email", "carla@example.com"), Map.class)
				.getBody().get("id");
	}

	private TestRestTemplate asAdmin() {
		return restTemplate.withBasicAuth("admin", "admin-password");
	}

	private long createIncident(String subject) {
		return createIncident(Map.of("subject", subject, "requesterId", requesterId));
	}

	private long createIncident(Map<String, Object> body) {
		ResponseEntity<Map> created = asAdmin().postForEntity("/api/incidents", body, Map.class);
		assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		return ((Number) created.getBody().get("id")).longValue();
	}

	private Ticket ticket(long id) {
		return ticketRepository.findById(id).orElseThrow();
	}

	private Task currentTask(long ticketId) {
		return taskService.createTaskQuery().processInstanceBusinessKey(String.valueOf(ticketId)).singleResult();
	}

	/**
	 * What an action will do once #111 exposes it: complete the stage's task with
	 * an outcome.
	 */
	private void act(long ticketId, String outcome) {
		taskService.complete(currentTask(ticketId).getId(), Map.of("outcome", outcome));
	}

	private long runningInstances(long ticketId) {
		return runtimeService.createProcessInstanceQuery().processDefinitionKey(IncidentCommandService.PROCESS_KEY)
				.processInstanceBusinessKey(String.valueOf(ticketId)).count();
	}

	@Test
	void creatingAnIncidentStartsExactlyOneProcessInstanceInTriage() {
		long id = createIncident("Printer on fire");

		assertThat(runningInstances(id)).isEqualTo(1);
		assertThat(currentTask(id).getTaskDefinitionKey()).isEqualTo("triage");
		assertThat(ticket(id).getStatus()).isEqualTo(TicketStatus.OPEN);
	}

	@Test
	void eachStageProjectsItsStatus() {
		long id = createIncident("Stage by stage");

		act(id, "start-work");
		assertThat(currentTask(id).getTaskDefinitionKey()).isEqualTo("work");
		assertThat(ticket(id).getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);

		act(id, "put-on-hold");
		assertThat(currentTask(id).getTaskDefinitionKey()).isEqualTo("on-hold");
		assertThat(ticket(id).getStatus()).isEqualTo(TicketStatus.PENDING);

		act(id, "resume");
		assertThat(ticket(id).getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);

		act(id, "resolve");
		assertThat(currentTask(id).getTaskDefinitionKey()).isEqualTo("confirm-resolution");
		assertThat(ticket(id).getStatus()).isEqualTo(TicketStatus.RESOLVED);
	}

	@Test
	void resolvingStampsResolvedAtReopeningClearsItAndClosingEndsTheProcess() {
		long id = createIncident("Resolved, reopened, closed");
		act(id, "start-work");

		act(id, "resolve");
		assertThat(ticket(id).getResolvedAt()).isNotNull();

		act(id, "reopen");
		assertThat(ticket(id).getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
		assertThat(ticket(id).getResolvedAt()).isNull();

		act(id, "resolve");
		Instant resolvedAt = ticket(id).getResolvedAt();
		act(id, "close");

		Ticket closed = ticket(id);
		assertThat(closed.getStatus()).isEqualTo(TicketStatus.CLOSED);
		assertThat(closed.getClosedAt()).isNotNull();
		assertThat(closed.getResolvedAt()).isEqualTo(resolvedAt);
		// closed is terminal: the process has ended
		assertThat(runningInstances(id)).isZero();
	}

	@Test
	void cancellingClosesWithoutResolving() {
		long id = createIncident("Raised by mistake");

		act(id, "cancel");

		Ticket cancelled = ticket(id);
		assertThat(cancelled.getStatus()).isEqualTo(TicketStatus.CLOSED);
		assertThat(cancelled.getClosedAt()).isNotNull();
		assertThat(cancelled.getResolvedAt()).isNull();
		assertThat(runningInstances(id)).isZero();
	}

	/** The seeded 1 - High x 1 - High pair derives P1, which has an SLA policy. */
	@Test
	void holdingPausesTheSlaClockAndResumingShiftsTheDeadlines() {
		long impactId = jdbcTemplate.queryForObject("SELECT id FROM impact WHERE name = '1 - High'", Long.class);
		long urgencyId = jdbcTemplate.queryForObject("SELECT id FROM urgency WHERE name = '1 - High'", Long.class);
		long id = createIncident(Map.of("subject", "Waiting on a vendor", "requesterId", requesterId, "impactId",
				impactId, "urgencyId", urgencyId));
		act(id, "start-work");
		Instant originalRespondBy = ticket(id).getRespondBy();
		assertThat(originalRespondBy).isNotNull();

		act(id, "put-on-hold");
		assertThat(ticket(id).getPendingSince()).isNotNull();

		// backdate the pause so the shift is observable
		Ticket paused = ticket(id);
		paused.setPendingSince(Instant.now().minus(Duration.ofHours(1)));
		ticketRepository.save(paused);

		act(id, "resume");

		Ticket resumed = ticket(id);
		assertThat(resumed.getPendingSince()).isNull();
		assertThat(Duration.between(originalRespondBy, resumed.getRespondBy()))
				.isGreaterThanOrEqualTo(Duration.ofMinutes(59));
	}

	@Test
	void everyStatusChangeIsPublishedWithItsExplicitPreviousStatus() {
		long id = createIncident("Watched");

		act(id, "start-work");

		List<TicketStatusChangedEvent> changes = events.stream(TicketStatusChangedEvent.class)
				.filter(event -> event.ticketId() == id).toList();
		assertThat(changes)
				.containsExactly(new TicketStatusChangedEvent(id, TicketStatus.OPEN, TicketStatus.IN_PROGRESS));
	}

	@Test
	void softDeletingAnIncidentRemovesItsProcessInstance() {
		long id = createIncident("To be deleted");

		asAdmin().delete("/api/incidents/" + id);

		assertThat(runningInstances(id)).isZero();
		assertThat(historyService.createHistoricProcessInstanceQuery().processInstanceBusinessKey(String.valueOf(id))
				.singleResult().getDeleteReason()).isEqualTo("Incident deleted");
	}

	/**
	 * Deleting a closed Incident has no running process left to end, and must not
	 * fail for it.
	 */
	@Test
	void softDeletingAClosedIncidentStillWorks() {
		long id = createIncident("Closed, then deleted");
		act(id, "cancel");

		asAdmin().delete("/api/incidents/" + id);

		assertThat(asAdmin().getForEntity("/api/incidents/" + id, String.class).getStatusCode())
				.isEqualTo(HttpStatus.NOT_FOUND);
	}

	/**
	 * ADR-0006: Flowable creates its own tables, IDM's stay absent, Flyway never
	 * touches either.
	 */
	@Test
	void flowableOwnsItsTablesAndFlywayNeverReferencesThem() {
		Integer engineTables = jdbcTemplate.queryForObject(
				"SELECT count(*) FROM information_schema.tables WHERE table_name IN ('act_ru_task', 'act_ru_execution', 'act_hi_procinst')",
				Integer.class);
		Integer identityTables = jdbcTemplate.queryForObject(
				"SELECT count(*) FROM information_schema.tables WHERE table_name LIKE 'act\\_id\\_%'", Integer.class);
		Integer flywayMentions = jdbcTemplate.queryForObject(
				"SELECT count(*) FROM flyway_schema_history WHERE script ILIKE '%act\\_%' OR description ILIKE '%flowable%'",
				Integer.class);

		assertThat(engineTables).isEqualTo(3);
		assertThat(identityTables).isZero();
		assertThat(flywayMentions).isZero();
	}

}
