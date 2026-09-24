package dev.alkolhar.servdesk.directory;

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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PersonControllerTest {

	@Autowired
	private TestRestTemplate restTemplate;

	private Number adminId;

	@BeforeAll
	void bootstrapAgent() {
		ResponseEntity<Map> created = restTemplate.postForEntity("/api/setup",
				new SetupRequest("Administrator", "admin@example.com", null, "admin", "admin123"), Map.class);
		adminId = (Number) created.getBody().get("id");
	}

	private TestRestTemplate asAdmin() {
		return restTemplate.withBasicAuth("admin", "admin123");
	}

	@Test
	void requestsWithoutCredentialsAreRejected() {
		ResponseEntity<String> response = restTemplate.getForEntity("/api/persons", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void createsAndReadsACustomer() {
		Map<String, Object> request = Map.of("role", "CUSTOMER", "name", "Carl Customer", "email", "carl@example.com");
		ResponseEntity<Map> created = asAdmin().postForEntity("/api/persons", request, Map.class);
		assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(created.getBody()).doesNotContainKey("password");
		assertThat(created.getBody().get("name")).isEqualTo("Carl Customer");
		Number id = (Number) created.getBody().get("id");

		ResponseEntity<Map> fetched = asAdmin().getForEntity("/api/persons/" + id, Map.class);
		assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(fetched.getBody().get("email")).isEqualTo("carl@example.com");
	}

	@Test
	void rejectsInvalidPayload() {
		Map<String, Object> request = Map.of("role", "CUSTOMER", "name", "", "email", "not-an-email");
		ResponseEntity<String> response = asAdmin().postForEntity("/api/persons", request, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	void returnsNotFoundForMissingPerson() {
		ResponseEntity<String> response = asAdmin().getForEntity("/api/persons/999999", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void updatesAndDeletesAPerson() {
		Map<String, Object> createRequest = Map.of("role", "CUSTOMER", "name", "Temp Person", "email",
				"temp@example.com");
		Number id = (Number) asAdmin().postForEntity("/api/persons", createRequest, Map.class).getBody().get("id");

		Map<String, Object> updateRequest = Map.of("role", "CUSTOMER", "name", "Updated Name", "email",
				"temp@example.com", "enabled", true);
		ResponseEntity<Map> updated = asAdmin().exchange("/api/persons/" + id, org.springframework.http.HttpMethod.PUT,
				new org.springframework.http.HttpEntity<>(updateRequest), Map.class);
		assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(updated.getBody().get("name")).isEqualTo("Updated Name");

		asAdmin().delete("/api/persons/" + id);
		ResponseEntity<String> afterDelete = asAdmin().getForEntity("/api/persons/" + id, String.class);
		assertThat(afterDelete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	/**
	 * Issue #66: a PUT that mentions neither {@code username}, {@code password} nor
	 * {@code enabled} used to erase the first two and disable the third, answering
	 * 200 while locking the person out of the application. Asserting on the login
	 * rather than on the row is the point — the harm was an account that could no
	 * longer authenticate.
	 */
	@Test
	void updatingAPersonWithoutEchoingCredentialsLeavesTheLoginIntact() {
		Map<String, Object> createRequest = Map.of("role", "AGENT", "name", "Ida Agent", "email", "ida@example.com",
				"username", "ida", "password", "ida12345");
		Number id = (Number) asAdmin().postForEntity("/api/persons", createRequest, Map.class).getBody().get("id");
		assertThat(restTemplate.withBasicAuth("ida", "ida12345").getForEntity("/api/persons", String.class)
				.getStatusCode()).isEqualTo(HttpStatus.OK);

		Map<String, Object> rename = Map.of("role", "AGENT", "name", "Ida Renamed", "email", "ida@example.com");
		ResponseEntity<Map> updated = asAdmin().exchange("/api/persons/" + id, org.springframework.http.HttpMethod.PUT,
				new org.springframework.http.HttpEntity<>(rename), Map.class);
		assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(updated.getBody().get("name")).isEqualTo("Ida Renamed");
		assertThat(updated.getBody().get("username")).isEqualTo("ida");
		assertThat(updated.getBody().get("enabled")).isEqualTo(true);

		assertThat(restTemplate.withBasicAuth("ida", "ida12345").getForEntity("/api/persons", String.class)
				.getStatusCode()).isEqualTo(HttpStatus.OK);

		// a second login-capable agent would change the count the last-agent tests
		// below depend on, so this one does not outlive its test
		asAdmin().delete("/api/persons/" + id);
	}

	/**
	 * Issue #63: a deployment must never lose its last Agent who can still log in.
	 * All three doors are shut — deleting them, demoting them to CUSTOMER, and
	 * disabling them — because {@code /api/setup} cannot recover the situation: it
	 * only runs while no person exists at all, and a bricked deployment still has
	 * rows. 409 rather than 403: the caller is a fully authorised Agent, so nothing
	 * about the caller is the problem.
	 */
	@Test
	void theLastLoginCapableAgentCannotBeDeletedDemotedOrDisabled() {
		ResponseEntity<String> deleted = asAdmin().exchange("/api/persons/" + adminId,
				org.springframework.http.HttpMethod.DELETE, null, String.class);
		assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

		Map<String, Object> demotion = Map.of("role", "CUSTOMER", "name", "Administrator", "email",
				"admin@example.com");
		assertThat(putAdmin(demotion).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

		Map<String, Object> disable = Map.of("role", "AGENT", "name", "Administrator", "email", "admin@example.com",
				"enabled", false);
		assertThat(putAdmin(disable).getStatusCode()).isEqualTo(HttpStatus.CONFLICT);

		// the rejections left the account exactly as it was
		assertThat(asAdmin().getForEntity("/api/persons", String.class).getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	/**
	 * The invariant is about the last one, not about agents in general: a second
	 * credentialed agent is removable, and renaming the remaining one is untouched
	 * because it never costs them their login.
	 */
	@Test
	void anAgentIsRemovableWhileAnotherCanStillLogIn() {
		Map<String, Object> second = Map.of("role", "AGENT", "name", "Second Agent", "email", "second@example.com",
				"username", "second", "password", "second12345");
		Number id = (Number) asAdmin().postForEntity("/api/persons", second, Map.class).getBody().get("id");

		ResponseEntity<String> deleted = asAdmin().exchange("/api/persons/" + id,
				org.springframework.http.HttpMethod.DELETE, null, String.class);
		assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

		Map<String, Object> rename = Map.of("role", "AGENT", "name", "Administrator Renamed", "email",
				"admin@example.com");
		ResponseEntity<String> renamed = putAdmin(rename);
		assertThat(renamed.getStatusCode()).isEqualTo(HttpStatus.OK);

		// put the name back so the rest of the class sees the fixture it expects
		putAdmin(Map.of("role", "AGENT", "name", "Administrator", "email", "admin@example.com"));
	}

	private ResponseEntity<String> putAdmin(Map<String, Object> body) {
		return asAdmin().exchange("/api/persons/" + adminId, org.springframework.http.HttpMethod.PUT,
				new org.springframework.http.HttpEntity<>(body), String.class);
	}

	/**
	 * The unique indexes on soft-deletable columns are partial
	 * ({@code WHERE deleted_at IS NULL} — see {@code V1__init_schema.sql}), so a
	 * soft-deleted person's email is free for reuse instead of squatting on the
	 * index and turning the recreate into a 409.
	 */
	@Test
	void recreatingASoftDeletedPersonsEmailSucceeds() {
		Map<String, Object> request = Map.of("role", "CUSTOMER", "name", "Riley Recreated", "email",
				"riley@example.com");
		Number id = (Number) asAdmin().postForEntity("/api/persons", request, Map.class).getBody().get("id");
		asAdmin().delete("/api/persons/" + id);

		ResponseEntity<Map> recreated = asAdmin().postForEntity("/api/persons", request, Map.class);
		assertThat(recreated.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(recreated.getBody().get("email")).isEqualTo("riley@example.com");
	}

	@Test
	void customersCannotManageThePersonDirectory() {
		Map<String, Object> customerRequest = Map.of("role", "CUSTOMER", "name", "Cara Customer", "email",
				"cara@example.com", "username", "cara", "password", "cara12345");
		asAdmin().postForEntity("/api/persons", customerRequest, Map.class);

		ResponseEntity<String> response = restTemplate.withBasicAuth("cara", "cara12345").getForEntity("/api/persons",
				String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}
}
