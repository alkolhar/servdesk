package dev.alkolhar.servdesk.classification;

import static org.assertj.core.api.Assertions.assertThat;

import dev.alkolhar.servdesk.TestcontainersConfiguration;
import dev.alkolhar.servdesk.setup.SetupRequest;
import java.util.HashMap;
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

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PriorityDefinitionControllerTest {

	@Autowired
	private TestRestTemplate restTemplate;

	private Number impactId;
	private Number urgencyId;
	private Number priorityId;

	@BeforeAll
	void bootstrapFixtures() {
		restTemplate.postForEntity("/api/setup",
				new SetupRequest("Administrator", "admin@example.com", null, "admin", "admin123"), String.class);
		Map<String, Object> customerRequest = Map.of("role", "CUSTOMER", "name", "Carla Customer", "email",
				"carla@example.com", "username", "carla", "password", "carla12345");
		asAdmin().postForEntity("/api/persons", customerRequest, Map.class);

		impactId = (Number) asAdmin().postForEntity("/api/impacts", Map.of("name", "High", "sortOrder", 0), Map.class)
				.getBody().get("id");
		urgencyId = (Number) asAdmin()
				.postForEntity("/api/urgencies", Map.of("name", "High", "sortOrder", 0), Map.class).getBody().get("id");
		priorityId = (Number) asAdmin()
				.postForEntity("/api/priorities", Map.of("name", "Critical", "sortOrder", 0), Map.class).getBody()
				.get("id");
	}

	private TestRestTemplate asAdmin() {
		return restTemplate.withBasicAuth("admin", "admin123");
	}

	private TestRestTemplate asCustomer() {
		return restTemplate.withBasicAuth("carla", "carla12345");
	}

	private Map<String, Object> body(Number impactId, Number urgencyId, Number priorityId) {
		Map<String, Object> body = new HashMap<>();
		body.put("impactId", impactId);
		body.put("urgencyId", urgencyId);
		body.put("priorityId", priorityId);
		return body;
	}

	@Test
	void requestsWithoutCredentialsAreRejected() {
		ResponseEntity<String> response = restTemplate.getForEntity("/api/priority-definitions", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void createsAndReadsAPriorityDefinition() {
		ResponseEntity<Map> created = asAdmin().postForEntity("/api/priority-definitions",
				body(impactId, urgencyId, priorityId), Map.class);
		assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(((Number) created.getBody().get("impactId")).longValue()).isEqualTo(impactId.longValue());
		assertThat(((Number) created.getBody().get("urgencyId")).longValue()).isEqualTo(urgencyId.longValue());
		assertThat(((Number) created.getBody().get("priorityId")).longValue()).isEqualTo(priorityId.longValue());
		Number id = (Number) created.getBody().get("id");

		ResponseEntity<Map> fetched = asAdmin().getForEntity("/api/priority-definitions/" + id, Map.class);
		assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
	}

	@Test
	void rejectsADuplicateImpactUrgencyPair() {
		Number ownImpactId = (Number) asAdmin()
				.postForEntity("/api/impacts", Map.of("name", "Duplicate-check impact", "sortOrder", 1), Map.class)
				.getBody().get("id");
		ResponseEntity<Map> first = asAdmin().postForEntity("/api/priority-definitions",
				body(ownImpactId, urgencyId, priorityId), Map.class);
		assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);

		ResponseEntity<String> duplicate = asAdmin().postForEntity("/api/priority-definitions",
				body(ownImpactId, urgencyId, priorityId), String.class);
		assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
	}

	@Test
	void returnsNotFoundForMissingPriorityDefinition() {
		ResponseEntity<String> response = asAdmin().getForEntity("/api/priority-definitions/999999", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void updatesAndDeletesAPriorityDefinition() {
		Number ownImpactId = (Number) asAdmin()
				.postForEntity("/api/impacts", Map.of("name", "Update-check impact", "sortOrder", 2), Map.class)
				.getBody().get("id");
		Number id = (Number) asAdmin()
				.postForEntity("/api/priority-definitions", body(ownImpactId, urgencyId, priorityId), Map.class)
				.getBody().get("id");

		Number otherPriorityId = (Number) asAdmin()
				.postForEntity("/api/priorities", Map.of("name", "Update-check priority", "sortOrder", 5), Map.class)
				.getBody().get("id");
		ResponseEntity<Map> updated = asAdmin().exchange("/api/priority-definitions/" + id, HttpMethod.PUT,
				new HttpEntity<>(body(ownImpactId, urgencyId, otherPriorityId)), Map.class);
		assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(((Number) updated.getBody().get("priorityId")).longValue()).isEqualTo(otherPriorityId.longValue());

		asAdmin().delete("/api/priority-definitions/" + id);
		ResponseEntity<String> afterDelete = asAdmin().getForEntity("/api/priority-definitions/" + id, String.class);
		assertThat(afterDelete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void customersCanReadButNotCreateUpdateOrDelete() {
		Number ownImpactId = (Number) asAdmin()
				.postForEntity("/api/impacts", Map.of("name", "RBAC-check impact", "sortOrder", 3), Map.class).getBody()
				.get("id");
		Number id = (Number) asAdmin()
				.postForEntity("/api/priority-definitions", body(ownImpactId, urgencyId, priorityId), Map.class)
				.getBody().get("id");

		ResponseEntity<String> createResponse = asCustomer().postForEntity("/api/priority-definitions",
				body(ownImpactId, urgencyId, priorityId), String.class);
		assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

		ResponseEntity<Map> readResponse = asCustomer().getForEntity("/api/priority-definitions/" + id, Map.class);
		assertThat(readResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

		ResponseEntity<String> updateResponse = asCustomer().exchange("/api/priority-definitions/" + id, HttpMethod.PUT,
				new HttpEntity<>(body(ownImpactId, urgencyId, priorityId)), String.class);
		assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

		ResponseEntity<String> deleteResponse = asCustomer().exchange("/api/priority-definitions/" + id,
				HttpMethod.DELETE, null, String.class);
		assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}
}
