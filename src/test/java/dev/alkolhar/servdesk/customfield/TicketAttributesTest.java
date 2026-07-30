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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

/**
 * End-to-end proof of the custom-field mechanism on tickets (issue #29):
 * definitions declared through the real admin endpoint, values written and read
 * through a subtype endpoint (Incidents — the mechanism lives on the shared
 * {@code Ticket}, so one subtype suffices), rejections surfacing as 400, and
 * the jsonb-backed {@code attrKey}/{@code attrValue} filter on the
 * cross-subtype overview.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TicketAttributesTest {

	@Autowired
	private TestRestTemplate restTemplate;

	private Number requesterId;

	@BeforeAll
	void bootstrapFixtures() {
		restTemplate.postForEntity("/api/setup",
				new SetupRequest("Administrator", "admin@example.com", null, "admin", "admin123"), String.class);
		Map<String, Object> customerRequest = Map.of("role", "CUSTOMER", "name", "Carla Customer", "email",
				"carla@example.com", "username", "carla", "password", "carla12345");
		requesterId = (Number) asAdmin().postForEntity("/api/persons", customerRequest, Map.class).getBody().get("id");

		defineAttribute("environment", "ENUM", Map.of("enumValues", List.of("prod", "test")));
		defineAttribute("impacted_users", "NUMBER", Map.of());
		defineAttribute("po_number", "STRING", Map.of());
	}

	private void defineAttribute(String key, String type, Map<String, Object> extra) {
		Map<String, Object> request = new java.util.HashMap<>(
				Map.of("target", "TICKET", "key", key, "label", key, "type", type, "required", false));
		request.putAll(extra);
		ResponseEntity<Map> response = asAdmin().postForEntity("/api/attribute-definitions", request, Map.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
	}

	private TestRestTemplate asAdmin() {
		return restTemplate.withBasicAuth("admin", "admin123");
	}

	private Map<String, Object> incidentBody(String subject, Map<String, Object> attributes) {
		return Map.of("subject", subject, "requesterId", requesterId, "attributes", attributes);
	}

	@Test
	void attributesRoundTripThroughCreateAndRead() {
		Map<String, Object> attributes = Map.of("environment", "prod", "impacted_users", 250, "po_number", "PO-4711");
		ResponseEntity<Map> created = asAdmin().postForEntity("/api/incidents",
				incidentBody("Mail down in prod", attributes), Map.class);
		assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		Number id = (Number) created.getBody().get("id");

		Map<String, Object> fetched = asAdmin().getForEntity("/api/incidents/" + id, Map.class).getBody();
		Map<String, Object> roundTripped = (Map<String, Object>) fetched.get("attributes");
		assertThat(roundTripped).containsEntry("environment", "prod").containsEntry("po_number", "PO-4711");
		assertThat(((Number) roundTripped.get("impacted_users")).intValue()).isEqualTo(250);

		Map<String, Object> overview = asAdmin().getForEntity("/api/tickets/" + id, Map.class).getBody();
		assertThat((Map<String, Object>) overview.get("attributes")).containsEntry("environment", "prod");
	}

	@Test
	void rejectsInvalidAttributeValuesWith400() {
		assertThat(asAdmin()
				.postForEntity("/api/incidents", incidentBody("Unknown key", Map.of("no_such_key", "x")), String.class)
				.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(asAdmin().postForEntity("/api/incidents", incidentBody("Bad enum", Map.of("environment", "staging")),
				String.class).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
		assertThat(asAdmin().postForEntity("/api/incidents", incidentBody("Bad type", Map.of("impacted_users", "many")),
				String.class).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	void filtersTheOverviewByAttributeValue() {
		Number prodId = (Number) asAdmin().postForEntity("/api/incidents",
				incidentBody("Prod incident", Map.of("environment", "prod")), Map.class).getBody().get("id");
		Number testId = (Number) asAdmin().postForEntity("/api/incidents",
				incidentBody("Test incident", Map.of("environment", "test")), Map.class).getBody().get("id");

		ResponseEntity<Map> filtered = asAdmin()
				.getForEntity("/api/tickets?size=100&attrKey=environment&attrValue=prod", Map.class);
		assertThat(filtered.getStatusCode()).isEqualTo(HttpStatus.OK);
		Map<String, Object> embedded = (Map<String, Object>) filtered.getBody().get("_embedded");
		List<Map> tickets = (List<Map>) embedded.get("ticketModelList");
		List<Long> ids = tickets.stream().map(ticket -> ((Number) ticket.get("id")).longValue()).toList();
		assertThat(ids).contains(prodId.longValue()).doesNotContain(testId.longValue());
	}

	@Test
	void rejectsAnAttrKeyWithoutAnAttrValue() {
		assertThat(asAdmin().getForEntity("/api/tickets?attrKey=environment", String.class).getStatusCode())
				.isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	void requiredAttributesAreEnforcedOnWrite() {
		defineAttribute("triage_note", "STRING", Map.of("required", true));
		try {
			assertThat(
					asAdmin().postForEntity("/api/incidents", incidentBody("Missing required", Map.of()), String.class)
							.getStatusCode())
					.isEqualTo(HttpStatus.BAD_REQUEST);
			assertThat(
					asAdmin()
							.postForEntity("/api/incidents",
									incidentBody("Has required", Map.of("triage_note", "checked")), Map.class)
							.getStatusCode())
					.isEqualTo(HttpStatus.CREATED);
		} finally {
			// soft-delete the definition so the other tests' fixtures stay valid
			Number definitionId = definitionIdOf("triage_note");
			asAdmin().delete("/api/attribute-definitions/" + definitionId);
		}
	}

	@SuppressWarnings("unchecked")
	private Number definitionIdOf(String key) {
		Map<String, Object> body = asAdmin().getForEntity("/api/attribute-definitions?target=TICKET", Map.class)
				.getBody();
		Map<String, Object> embedded = (Map<String, Object>) body.get("_embedded");
		List<Map> definitions = (List<Map>) embedded.get("attributeDefinitionModelList");
		return definitions.stream().filter(definition -> key.equals(definition.get("key")))
				.map(definition -> (Number) definition.get("id")).findFirst().orElseThrow();
	}
}
