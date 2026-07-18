package dev.alkolhar.servdesk.ticket;

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

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TicketControllerTest {

	@Autowired
	private TestRestTemplate restTemplate;

	private Number requesterId;
	private Number assigneeId;

	@BeforeAll
	void bootstrapAgentAndPeople() {
		restTemplate.postForEntity("/api/setup",
				new SetupRequest("Administrator", "admin@example.com", null, "admin", "admin123"), String.class);

		Map<String, Object> customer = Map.of("role", "CUSTOMER", "name", "Carl Customer", "email", "carl@example.com");
		requesterId = (Number) asAdmin().postForEntity("/api/persons", customer, Map.class).getBody().get("id");

		Map<String, Object> agent = Map.of("role", "AGENT", "name", "Ada Agent", "email", "ada@example.com");
		assigneeId = (Number) asAdmin().postForEntity("/api/persons", agent, Map.class).getBody().get("id");
	}

	private TestRestTemplate asAdmin() {
		return restTemplate.withBasicAuth("admin", "admin123");
	}

	@Test
	void createsATicketWithAGeneratedNumberAndHypermediaLinks() {
		Map<String, Object> request = Map.of("type", "INCIDENT", "subject", "Printer on fire", "description",
				"Literally smoking", "requesterId", requesterId, "assigneeId", assigneeId);

		ResponseEntity<Map> created = asAdmin().postForEntity("/api/tickets", request, Map.class);
		assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		Map<String, Object> body = created.getBody();
		assertThat(body.get("ticketNumber").toString()).matches("TCK-\\d{6}");
		assertThat(body.get("status")).isEqualTo("OPEN");
		assertThat(body.get("requesterId")).isEqualTo(requesterId.intValue());

		@SuppressWarnings("unchecked")
		Map<String, Object> links = (Map<String, Object>) body.get("_links");
		assertThat(links).containsKeys("self", "requester", "assignee");
	}

	@Test
	void rejectsTicketWithoutRequiredFields() {
		Map<String, Object> request = Map.of("type", "INCIDENT", "subject", "");
		ResponseEntity<String> response = asAdmin().postForEntity("/api/tickets", request, String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	@Test
	void updatesTicketStatusAndDeletesIt() {
		Map<String, Object> createRequest = Map.of("type", "INCIDENT", "subject", "Broken chair", "requesterId",
				requesterId);
		Number id = (Number) asAdmin().postForEntity("/api/tickets", createRequest, Map.class).getBody().get("id");

		Map<String, Object> updateRequest = Map.of("type", "INCIDENT", "status", "RESOLVED", "subject",
				"Broken chair - fixed", "requesterId", requesterId, "resolvedAt", "2026-07-18T12:30:00Z");
		ResponseEntity<Map> updated = asAdmin().exchange("/api/tickets/" + id, HttpMethod.PUT,
				new HttpEntity<>(updateRequest), Map.class);
		assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(updated.getBody().get("status")).isEqualTo("RESOLVED");
		assertThat(updated.getBody().get("resolvedAt")).isNotNull();

		asAdmin().delete("/api/tickets/" + id);
		ResponseEntity<String> afterDelete = asAdmin().getForEntity("/api/tickets/" + id, String.class);
		assertThat(afterDelete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void returnsNotFoundForMissingTicket() {
		ResponseEntity<String> response = asAdmin().getForEntity("/api/tickets/999999", String.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void customersCanReadAndCreateButNotModifyOrDeleteTickets() {
		Map<String, Object> customerRequest = Map.of("role", "CUSTOMER", "name", "Cara Customer", "email",
				"cara@example.com", "username", "cara", "password", "cara12345");
		Number customerId = (Number) asAdmin().postForEntity("/api/persons", customerRequest, Map.class).getBody()
				.get("id");
		TestRestTemplate asCustomer = restTemplate.withBasicAuth("cara", "cara12345");

		Map<String, Object> createRequest = Map.of("type", "INCIDENT", "subject", "Customer-raised ticket",
				"requesterId", customerId);
		ResponseEntity<Map> created = asCustomer.postForEntity("/api/tickets", createRequest, Map.class);
		assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		Number id = (Number) created.getBody().get("id");

		ResponseEntity<Map> read = asCustomer.getForEntity("/api/tickets/" + id, Map.class);
		assertThat(read.getStatusCode()).isEqualTo(HttpStatus.OK);

		Map<String, Object> updateRequest = Map.of("type", "INCIDENT", "status", "RESOLVED", "subject",
				"Customer-raised ticket", "requesterId", customerId);
		ResponseEntity<String> updateAttempt = asCustomer.exchange("/api/tickets/" + id, HttpMethod.PUT,
				new HttpEntity<>(updateRequest), String.class);
		assertThat(updateAttempt.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

		ResponseEntity<Void> deleteAttempt = asCustomer.exchange("/api/tickets/" + id, HttpMethod.DELETE, null,
				Void.class);
		assertThat(deleteAttempt.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
	}
}
