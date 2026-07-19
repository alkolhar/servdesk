package dev.alkolhar.servdesk.ticket;

import static org.assertj.core.api.Assertions.assertThat;

import dev.alkolhar.servdesk.TestcontainersConfiguration;
import dev.alkolhar.servdesk.setup.SetupRequest;
import java.util.HashMap;
import java.util.List;
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
 * Holds the scenarios common to every ticket subtype once (ADR-0001), driven
 * through the real HTTP/security/persistence stack exactly like
 * {@code PersonControllerTest}: create/read/update/delete round-trip, RBAC per
 * role (a Customer's create attempt now expects 403, a narrowing from the old
 * flat {@code Ticket}'s policy), the subtype's own display-number
 * prefix/format, {@code resolvedAt}/{@code closedAt} server-derivation on a
 * status transition (and clearing on reopen), soft-delete, and the shared
 * Comment endpoint's internal-flag-is-Agent-only enforcement.
 * {@code IncidentControllerTest}/{@code ProblemControllerTest}/
 * {@code ChangeControllerTest}/{@code ServiceRequestControllerTest} each extend
 * this, supplying only their own endpoint path and expected prefix. Genuinely
 * subtype-only behavior (currently just {@code Incident.relatedProblem}) is
 * covered by its own separate, small test class instead.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public abstract class AbstractTicketSubtypeControllerTest {

	@Autowired
	protected TestRestTemplate restTemplate;

	protected Number requesterId;

	protected abstract String basePath();

	protected abstract String expectedDisplayNumberPrefix();

	@BeforeAll
	void bootstrapFixtures() {
		restTemplate.postForEntity("/api/setup",
				new SetupRequest("Administrator", "admin@example.com", null, "admin", "admin123"), String.class);
		Map<String, Object> customerRequest = Map.of("role", "CUSTOMER", "name", "Carla Customer", "email",
				"carla@example.com", "username", "carla", "password", "carla12345");
		requesterId = (Number) asAdmin().postForEntity("/api/persons", customerRequest, Map.class).getBody().get("id");
	}

	protected TestRestTemplate asAdmin() {
		return restTemplate.withBasicAuth("admin", "admin123");
	}

	protected TestRestTemplate asCustomer() {
		return restTemplate.withBasicAuth("carla", "carla12345");
	}

	private Map<String, Object> createBody(String subject) {
		Map<String, Object> body = new HashMap<>();
		body.put("subject", subject);
		body.put("requesterId", requesterId);
		return body;
	}

	@Test
	void requestsWithoutCredentialsAreRejected() {
		ResponseEntity<String> response = restTemplate.getForEntity(basePath(), String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void createsAndReadsATicketWithItsOwnDisplayNumberPrefix() {
		ResponseEntity<Map> created = asAdmin().postForEntity(basePath(), createBody("Printer on fire"), Map.class);
		assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(created.getBody().get("status")).isEqualTo("OPEN");
		assertThat((String) created.getBody().get("displayNumber")).startsWith(expectedDisplayNumberPrefix());
		Number id = (Number) created.getBody().get("id");

		ResponseEntity<Map> fetched = asAdmin().getForEntity(basePath() + "/" + id, Map.class);
		assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(fetched.getBody().get("subject")).isEqualTo("Printer on fire");
		assertThat(((Number) fetched.getBody().get("requesterId")).longValue()).isEqualTo(requesterId.longValue());
	}

	@Test
	void rejectsInvalidPayload() {
		Map<String, Object> body = createBody("");
		ResponseEntity<String> response = asAdmin().postForEntity(basePath(), body, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	void returnsNotFoundForMissingTicket() {
		ResponseEntity<String> response = asAdmin().getForEntity(basePath() + "/999999", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void customersCanReadButNotCreateUpdateOrDelete() {
		Number id = (Number) asAdmin().postForEntity(basePath(), createBody("Customer RBAC fixture"), Map.class)
				.getBody().get("id");

		ResponseEntity<String> createResponse = asCustomer().postForEntity(basePath(), createBody("Not allowed"),
				String.class);
		assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

		ResponseEntity<Map> readResponse = asCustomer().getForEntity(basePath() + "/" + id, Map.class);
		assertThat(readResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

		Map<String, Object> updateBody = createBody("Not allowed");
		updateBody.put("status", "IN_PROGRESS");
		ResponseEntity<String> updateResponse = asCustomer().exchange(basePath() + "/" + id, HttpMethod.PUT,
				new HttpEntity<>(updateBody), String.class);
		assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

		ResponseEntity<String> deleteResponse = asCustomer().exchange(basePath() + "/" + id, HttpMethod.DELETE, null,
				String.class);
		assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}

	@Test
	void updateSetsResolvedAtOnResolutionAndClearsItOnReopen() {
		Number id = (Number) asAdmin().postForEntity(basePath(), createBody("Needs resolving"), Map.class).getBody()
				.get("id");

		Map<String, Object> resolveBody = createBody("Needs resolving");
		resolveBody.put("status", "RESOLVED");
		ResponseEntity<Map> resolved = asAdmin().exchange(basePath() + "/" + id, HttpMethod.PUT,
				new HttpEntity<>(resolveBody), Map.class);
		assertThat(resolved.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(resolved.getBody().get("resolvedAt")).isNotNull();

		Map<String, Object> reopenBody = createBody("Needs resolving");
		reopenBody.put("status", "IN_PROGRESS");
		ResponseEntity<Map> reopened = asAdmin().exchange(basePath() + "/" + id, HttpMethod.PUT,
				new HttpEntity<>(reopenBody), Map.class);
		assertThat(reopened.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(reopened.getBody().get("resolvedAt")).isNull();
	}

	@Test
	void deletesATicketAndItDisappearsFromLookups() {
		Number id = (Number) asAdmin().postForEntity(basePath(), createBody("To be deleted"), Map.class).getBody()
				.get("id");

		asAdmin().exchange(basePath() + "/" + id, HttpMethod.DELETE, null, Void.class);

		ResponseEntity<String> afterDelete = asAdmin().getForEntity(basePath() + "/" + id, String.class);
		assertThat(afterDelete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void addsCommentsAndRestrictsTheInternalFlagToAgents() {
		Number id = (Number) asAdmin().postForEntity(basePath(), createBody("Needs a comment"), Map.class).getBody()
				.get("id");
		String commentsPath = "/api/tickets/" + id + "/comments";

		ResponseEntity<String> rejectedInternal = asCustomer().postForEntity(commentsPath,
				Map.of("body", "Sneaky internal note", "internal", true), String.class);
		assertThat(rejectedInternal.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

		ResponseEntity<Map> customerReply = asCustomer().postForEntity(commentsPath,
				Map.of("body", "Any update?", "internal", false), Map.class);
		assertThat(customerReply.getStatusCode()).isEqualTo(HttpStatus.CREATED);

		ResponseEntity<Map> agentInternalNote = asAdmin().postForEntity(commentsPath,
				Map.of("body", "Working on it internally", "internal", true), Map.class);
		assertThat(agentInternalNote.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(agentInternalNote.getBody().get("internal")).isEqualTo(true);

		ResponseEntity<Map> asSeenByCustomer = asCustomer().getForEntity(commentsPath, Map.class);
		List<Map> customerVisible = embeddedComments(asSeenByCustomer);
		assertThat(customerVisible).hasSize(1);
		assertThat(customerVisible).allSatisfy(comment -> assertThat(comment.get("internal")).isEqualTo(false));

		ResponseEntity<Map> asSeenByAgent = asAdmin().getForEntity(commentsPath, Map.class);
		List<Map> agentVisible = embeddedComments(asSeenByAgent);
		assertThat(agentVisible).hasSize(2);
	}

	@SuppressWarnings("unchecked")
	private List<Map> embeddedComments(ResponseEntity<Map> response) {
		Map<String, Object> embedded = (Map<String, Object>) response.getBody().get("_embedded");
		return (List<Map>) embedded.get("commentModelList");
	}
}
