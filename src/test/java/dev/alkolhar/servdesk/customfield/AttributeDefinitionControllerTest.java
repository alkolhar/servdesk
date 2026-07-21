package dev.alkolhar.servdesk.customfield;

import static org.assertj.core.api.Assertions.assertThat;

import dev.alkolhar.servdesk.TestcontainersConfiguration;
import dev.alkolhar.servdesk.setup.SetupRequest;
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

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AttributeDefinitionControllerTest {

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
	void createsReadsUpdatesAndDeletesADefinition() {
		Map<String, Object> createRequest = Map.of("target", "TICKET", "key", "cost_center", "label", "Cost center",
				"type", "STRING", "required", false);
		ResponseEntity<Map> created = asAdmin().postForEntity("/api/attribute-definitions", createRequest, Map.class);
		assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(created.getBody().get("key")).isEqualTo("cost_center");
		Number id = (Number) created.getBody().get("id");

		Map<String, Object> updateRequest = Map.of("label", "Cost centre", "required", true);
		ResponseEntity<Map> updated = asAdmin().exchange("/api/attribute-definitions/" + id, HttpMethod.PUT,
				new HttpEntity<>(updateRequest), Map.class);
		assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(updated.getBody().get("label")).isEqualTo("Cost centre");
		assertThat(updated.getBody().get("required")).isEqualTo(true);
		// key/type immutable: the update request can't even carry them
		assertThat(updated.getBody().get("key")).isEqualTo("cost_center");

		asAdmin().delete("/api/attribute-definitions/" + id);
		assertThat(asAdmin().getForEntity("/api/attribute-definitions/" + id, String.class).getStatusCode())
				.isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void rejectsADuplicateKeyPerTargetWith409() {
		Map<String, Object> request = Map.of("target", "TICKET", "key", "twice", "label", "Twice", "type", "STRING",
				"required", false);
		assertThat(asAdmin().postForEntity("/api/attribute-definitions", request, Map.class).getStatusCode())
				.isEqualTo(HttpStatus.CREATED);
		assertThat(asAdmin().postForEntity("/api/attribute-definitions", request, String.class).getStatusCode())
				.isEqualTo(HttpStatus.CONFLICT);
	}

	@Test
	void rejectsMalformedDefinitions() {
		// non-machine key
		assertThat(asAdmin().postForEntity("/api/attribute-definitions",
				Map.of("target", "TICKET", "key", "Not A Key", "label", "x", "type", "STRING", "required", false),
				String.class).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		// ENUM without values
		assertThat(asAdmin().postForEntity("/api/attribute-definitions",
				Map.of("target", "TICKET", "key", "env", "label", "Env", "type", "ENUM", "required", false),
				String.class).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		// enumValues on a non-ENUM type
		assertThat(
				asAdmin().postForEntity("/api/attribute-definitions",
						Map.of("target", "TICKET", "key", "env2", "label", "Env", "type", "STRING", "required", false,
								"enumValues", List.of("a")),
						String.class).getStatusCode())
				.isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	void customersCanReadButNotWriteDefinitions() {
		asAdmin().postForEntity("/api/attribute-definitions",
				Map.of("target", "TICKET", "key", "readable", "label", "Readable", "type", "STRING", "required", false),
				Map.class);

		assertThat(asCustomer().getForEntity("/api/attribute-definitions?target=TICKET", Map.class).getStatusCode())
				.isEqualTo(HttpStatus.OK);
		assertThat(asCustomer().postForEntity("/api/attribute-definitions",
				Map.of("target", "TICKET", "key", "nope", "label", "Nope", "type", "STRING", "required", false),
				String.class).getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}
}
