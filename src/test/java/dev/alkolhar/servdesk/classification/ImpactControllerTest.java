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
class ImpactControllerTest {

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
		ResponseEntity<String> response = restTemplate.getForEntity("/api/impacts", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	@Test
	void createsAndReadsAnImpact() {
		Map<String, Object> request = Map.of("name", "Critical", "sortOrder", 0);
		ResponseEntity<Map> created = asAdmin().postForEntity("/api/impacts", request, Map.class);
		assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(created.getBody().get("name")).isEqualTo("Critical");
		assertThat(created.getBody().get("sortOrder")).isEqualTo(0);
		Number id = (Number) created.getBody().get("id");

		ResponseEntity<Map> fetched = asAdmin().getForEntity("/api/impacts/" + id, Map.class);
		assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(fetched.getBody().get("name")).isEqualTo("Critical");
	}

	@Test
	void rejectsInvalidPayload() {
		Map<String, Object> request = Map.of("name", "", "sortOrder", 0);
		ResponseEntity<String> response = asAdmin().postForEntity("/api/impacts", request, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	void returnsNotFoundForMissingImpact() {
		ResponseEntity<String> response = asAdmin().getForEntity("/api/impacts/999999", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void updatesAndDeletesAnImpact() {
		Number id = (Number) asAdmin().postForEntity("/api/impacts", Map.of("name", "Temp", "sortOrder", 5), Map.class)
				.getBody().get("id");

		Map<String, Object> updateRequest = new HashMap<>();
		updateRequest.put("name", "Updated");
		updateRequest.put("sortOrder", 2);
		ResponseEntity<Map> updated = asAdmin().exchange("/api/impacts/" + id, HttpMethod.PUT,
				new HttpEntity<>(updateRequest), Map.class);
		assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(updated.getBody().get("name")).isEqualTo("Updated");
		assertThat(updated.getBody().get("sortOrder")).isEqualTo(2);

		asAdmin().delete("/api/impacts/" + id);
		ResponseEntity<String> afterDelete = asAdmin().getForEntity("/api/impacts/" + id, String.class);
		assertThat(afterDelete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void customersCanReadButNotCreateUpdateOrDelete() {
		Number id = (Number) asAdmin().postForEntity("/api/impacts", Map.of("name", "Low", "sortOrder", 9), Map.class)
				.getBody().get("id");

		ResponseEntity<String> createResponse = asCustomer().postForEntity("/api/impacts",
				Map.of("name", "Not allowed", "sortOrder", 1), String.class);
		assertThat(createResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

		ResponseEntity<Map> readResponse = asCustomer().getForEntity("/api/impacts/" + id, Map.class);
		assertThat(readResponse.getStatusCode()).isEqualTo(HttpStatus.OK);

		Map<String, Object> updateBody = new HashMap<>();
		updateBody.put("name", "Not allowed");
		updateBody.put("sortOrder", 1);
		ResponseEntity<String> updateResponse = asCustomer().exchange("/api/impacts/" + id, HttpMethod.PUT,
				new HttpEntity<>(updateBody), String.class);
		assertThat(updateResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

		ResponseEntity<String> deleteResponse = asCustomer().exchange("/api/impacts/" + id, HttpMethod.DELETE, null,
				String.class);
		assertThat(deleteResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}
}
