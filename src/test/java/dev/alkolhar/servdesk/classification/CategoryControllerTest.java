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
class CategoryControllerTest {

	@Autowired
	private TestRestTemplate restTemplate;

	@BeforeAll
	void bootstrapFixtures() {
		restTemplate.postForEntity("/api/setup",
				new SetupRequest("Administrator", "admin@example.com", null, "admin", "admin123"), String.class);
		Map<String, Object> customerRequest = Map.of("role", "CUSTOMER", "name", "Carla Customer", "email",
				"carla@example.com", "username", "carla", "password", "carla12345");
		asAdmin().postForEntity("/api/persons", customerRequest, Map.class);
	}

	private TestRestTemplate asAdmin() {
		return restTemplate.withBasicAuth("admin", "admin123");
	}

	private TestRestTemplate asCustomer() {
		return restTemplate.withBasicAuth("carla", "carla12345");
	}

	@Test
	void requestsWithoutCredentialsAreRejected() {
		ResponseEntity<String> response = restTemplate.getForEntity("/api/categories", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void createsAndReadsACategoryWithAParent() {
		Map<String, Object> parentRequest = Map.of("name", "Hardware");
		Number parentId = (Number) asAdmin().postForEntity("/api/categories", parentRequest, Map.class).getBody()
				.get("id");

		Map<String, Object> childRequest = new HashMap<>();
		childRequest.put("name", "Laptop");
		childRequest.put("parentId", parentId);
		ResponseEntity<Map> created = asAdmin().postForEntity("/api/categories", childRequest, Map.class);
		assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(created.getBody().get("name")).isEqualTo("Laptop");
		assertThat(((Number) created.getBody().get("parentId")).longValue()).isEqualTo(parentId.longValue());
		Number id = (Number) created.getBody().get("id");

		ResponseEntity<Map> fetched = asAdmin().getForEntity("/api/categories/" + id, Map.class);
		assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(fetched.getBody().get("name")).isEqualTo("Laptop");
	}

	@Test
	void rejectsInvalidPayload() {
		Map<String, Object> request = Map.of("name", "");
		ResponseEntity<String> response = asAdmin().postForEntity("/api/categories", request, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	void returnsNotFoundForMissingCategory() {
		ResponseEntity<String> response = asAdmin().getForEntity("/api/categories/999999", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void updatesAndDeletesACategory() {
		Number id = (Number) asAdmin().postForEntity("/api/categories", Map.of("name", "Temp"), Map.class).getBody()
				.get("id");

		Map<String, Object> updateRequest = new HashMap<>();
		updateRequest.put("name", "Updated");
		ResponseEntity<Map> updated = asAdmin().exchange("/api/categories/" + id, HttpMethod.PUT,
				new HttpEntity<>(updateRequest), Map.class);
		assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(updated.getBody().get("name")).isEqualTo("Updated");

		asAdmin().delete("/api/categories/" + id);
		ResponseEntity<String> afterDelete = asAdmin().getForEntity("/api/categories/" + id, String.class);
		assertThat(afterDelete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void customersCanReadButNotCreateUpdateOrDelete() {
		Number id = (Number) asAdmin().postForEntity("/api/categories", Map.of("name", "Software"), Map.class).getBody()
				.get("id");

		ResponseEntity<String> createResponse = asCustomer().postForEntity("/api/categories",
				Map.of("name", "Not allowed"), String.class);
		assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

		ResponseEntity<Map> readResponse = asCustomer().getForEntity("/api/categories/" + id, Map.class);
		assertThat(readResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

		Map<String, Object> updateBody = new HashMap<>();
		updateBody.put("name", "Not allowed");
		ResponseEntity<String> updateResponse = asCustomer().exchange("/api/categories/" + id, HttpMethod.PUT,
				new HttpEntity<>(updateBody), String.class);
		assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

		ResponseEntity<String> deleteResponse = asCustomer().exchange("/api/categories/" + id, HttpMethod.DELETE, null,
				String.class);
		assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}
}
