package dev.alkolhar.servdesk.ticket.incident;

import static org.assertj.core.api.Assertions.assertThat;

import dev.alkolhar.servdesk.TestcontainersConfiguration;
import dev.alkolhar.servdesk.setup.SetupRequest;
import java.util.List;
import java.util.Map;
import org.flowable.engine.HistoryService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.history.HistoricTaskInstance;
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
import org.springframework.test.annotation.DirtiesContext;

/**
 * Task-backed actions (ADR-0008) through the real HTTP stack, on the Incident
 * lifecycle: action links offered per stage and per caller, actions refused
 * outside their stage, required comments, and no Flowable task id anywhere in
 * the API.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IncidentActionsTest {

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private TaskService taskService;

	@Autowired
	private HistoryService historyService;

	private Number requesterId;
	private Number adminId;

	@BeforeAll
	void bootstrapFixtures() {
		adminId = (Number) restTemplate.postForEntity("/api/setup",
				new SetupRequest("Administrator", "admin@example.com", null, "admin", "admin-password"), Map.class)
				.getBody().get("id");
		requesterId = (Number) asAdmin()
				.postForEntity("/api/persons", Map.of("role", "CUSTOMER", "name", "Carla Customer", "email",
						"carla@example.com", "username", "carla", "password", "carla-password"), Map.class)
				.getBody().get("id");
	}

	private TestRestTemplate asAdmin() {
		return restTemplate.withBasicAuth("admin", "admin-password");
	}

	private TestRestTemplate asCustomer() {
		return restTemplate.withBasicAuth("carla", "carla-password");
	}

	private long createIncident(String subject) {
		ResponseEntity<Map> created = asAdmin().postForEntity("/api/incidents",
				Map.of("subject", subject, "requesterId", requesterId), Map.class);
		return ((Number) created.getBody().get("id")).longValue();
	}

	private Map<String, Object> incident(long id, TestRestTemplate as) {
		return as.getForEntity("/api/incidents/" + id, Map.class).getBody();
	}

	private static List<String> actionRels(Map<String, Object> model) {
		return ((Map<String, Object>) model.get("_links")).keySet().stream().filter(rel -> rel.startsWith("action:"))
				.sorted().toList();
	}

	private ResponseEntity<String> act(long id, String action) {
		return asAdmin().postForEntity("/api/tickets/" + id + "/actions/" + action, null, String.class);
	}

	private ResponseEntity<String> act(long id, String action, String comment) {
		return asAdmin().postForEntity("/api/tickets/" + id + "/actions/" + action, Map.of("comment", comment),
				String.class);
	}

	@Test
	void anAgentTakesAnIncidentFromOpenToClosedThroughItsLinks() {
		long id = createIncident("Printer on fire");

		Map<String, Object> open = incident(id, asAdmin());
		assertThat(open.get("status")).isEqualTo("OPEN");
		assertThat(actionRels(open)).containsExactly("action:cancel", "action:start-work");
		List<Map<String, Object>> tasks = (List<Map<String, Object>>) open.get("tasks");
		assertThat(tasks).singleElement().satisfies(task -> {
			assertThat(task.get("key")).isEqualTo("triage");
			assertThat(task.get("createdAt")).isNotNull();
			assertThat(((Map<String, Object>) task.get("_links")).keySet()).containsExactlyInAnyOrder("action:cancel",
					"action:start-work");
		});
		// the link is the URL: a client follows it rather than building one
		String startWork = (String) ((Map<String, Object>) ((Map<String, Object>) open.get("_links"))
				.get("action:start-work")).get("href");
		assertThat(asAdmin().postForEntity(startWork, null, String.class).getStatusCode())
				.isEqualTo(HttpStatus.NO_CONTENT);

		Map<String, Object> working = incident(id, asAdmin());
		assertThat(working.get("status")).isEqualTo("IN_PROGRESS");
		assertThat(actionRels(working)).containsExactly("action:cancel", "action:put-on-hold", "action:resolve");

		assertThat(act(id, "resolve", "Toner replaced").getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		assertThat(actionRels(incident(id, asAdmin()))).containsExactly("action:close", "action:reopen");

		assertThat(act(id, "close").getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		Map<String, Object> closed = incident(id, asAdmin());
		assertThat(closed.get("status")).isEqualTo("CLOSED");
		assertThat(closed.get("resolvedAt")).isNotNull();
		assertThat(closed.get("closedAt")).isNotNull();
		assertThat((List<?>) closed.get("tasks")).isEmpty();
		assertThat(actionRels(closed)).isEmpty();
	}

	@Test
	void anActionOutsideTheCurrentStageIsRefused() {
		long id = createIncident("Not so fast");

		ResponseEntity<String> tooEarly = act(id, "resolve", "Fixed before anyone looked");

		assertThat(tooEarly.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
		assertThat(incident(id, asAdmin()).get("status")).isEqualTo("OPEN");
		assertThat(act(id, "no-such-action").getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
	}

	@Test
	void aClosedIncidentOffersNothingAndRefusesEverything() {
		long id = createIncident("Already done");
		act(id, "cancel", "Raised by mistake");

		assertThat(act(id, "start-work").getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
	}

	@Test
	void resolveAndCancelNeedAComment() {
		long id = createIncident("Needs explaining");
		act(id, "start-work");

		assertThat(act(id, "resolve").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(act(id, "resolve", "   ").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(act(id, "cancel").getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		// nothing moved
		assertThat(incident(id, asAdmin()).get("status")).isEqualTo("IN_PROGRESS");

		assertThat(act(id, "resolve", "Cable reseated").getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		ResponseEntity<Map> comments = asAdmin().getForEntity("/api/tickets/" + id + "/comments", Map.class);
		List<Map<String, Object>> stream = (List<Map<String, Object>>) ((Map<String, Object>) comments.getBody()
				.get("_embedded")).values().iterator().next();
		assertThat(stream).singleElement().satisfies(comment -> {
			assertThat(comment.get("body")).isEqualTo("Cable reseated");
			assertThat(comment.get("internal")).isEqualTo(false);
		});
	}

	@Test
	void cancellingClosesWithoutResolving() {
		long id = createIncident("Duplicate");

		assertThat(act(id, "cancel", "Duplicate of INC-000001").getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

		Map<String, Object> cancelled = incident(id, asAdmin());
		assertThat(cancelled.get("status")).isEqualTo("CLOSED");
		assertThat(cancelled.get("closedAt")).isNotNull();
		assertThat(cancelled.get("resolvedAt")).isNull();
	}

	@Test
	void aCustomerSeesTheTaskButNoActionsAndCannotAct() {
		long id = createIncident("Carla's printer");

		Map<String, Object> asCarla = incident(id, asCustomer());
		assertThat(actionRels(asCarla)).isEmpty();
		assertThat((List<Map<String, Object>>) asCarla.get("tasks")).singleElement()
				.satisfies(task -> assertThat(task.get("_links")).isNull());

		ResponseEntity<String> refused = asCustomer().postForEntity("/api/tickets/" + id + "/actions/start-work", null,
				String.class);
		assertThat(refused.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	void anUnknownTicketIsA404() {
		assertThat(act(999_999, "start-work").getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	/** ADR-0008: the contract speaks tickets; the engine's task ids stay inside. */
	@Test
	void noFlowableTaskIdAppearsInTheApi() {
		long id = createIncident("Opaque");
		String taskId = taskService.createTaskQuery().processInstanceBusinessKey(String.valueOf(id)).singleResult()
				.getId();

		String body = asAdmin().getForEntity("/api/incidents/" + id, String.class).getBody();

		assertThat(body).doesNotContain(taskId);
	}

	/** ADR-0009: the engine's history says who acted, as a Person id. */
	@Test
	void theActingAgentIsRecordedOnTheCompletedTask() {
		long id = createIncident("Who did it");

		act(id, "start-work");

		HistoricTaskInstance triage = historyService.createHistoricTaskInstanceQuery()
				.processInstanceBusinessKey(String.valueOf(id)).taskDefinitionKey("triage").singleResult();
		assertThat(triage.getCompletedBy()).isEqualTo(String.valueOf(adminId));
	}

}
