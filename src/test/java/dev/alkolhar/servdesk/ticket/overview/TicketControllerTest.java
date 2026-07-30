package dev.alkolhar.servdesk.ticket.overview;

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
 * Integration coverage for the cross-subtype overview (issue #30): one ticket
 * of each subtype is created through the real subtype endpoints, then read back
 * through {@code /api/tickets} — type discrimination, display-number
 * pass-through, subtype links, filters, and the same row-level ownership rules
 * as everywhere else.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class TicketControllerTest {

	@Autowired
	private TestRestTemplate restTemplate;

	private Number customerId;
	private Number otherCustomerId;
	private Number incidentId;
	private Number problemId;
	private Number changeId;
	private Number serviceRequestId;
	private Number foreignIncidentId;

	@BeforeAll
	void bootstrapFixtures() {
		restTemplate.postForEntity("/api/setup",
				new SetupRequest("Administrator", "admin@example.com", null, "admin", "admin123"), String.class);
		customerId = createCustomer("Carla Customer", "carla@example.com", "carla", "carla12345");
		otherCustomerId = createCustomer("Otto Other", "otto@example.com", "otto", "otto12345");

		incidentId = createTicket("/api/incidents", "Mail down", customerId);
		problemId = createTicket("/api/problems", "Mail outages recurring", customerId);
		changeId = createTicket("/api/changes", "Replace mail relay", customerId);
		serviceRequestId = createTicket("/api/service-requests", "New mailbox", customerId);
		foreignIncidentId = createTicket("/api/incidents", "Otto's incident", otherCustomerId);
	}

	@Test
	void listsAllSubtypesWithTypeDiscriminatorAndDisplayNumber() {
		List<Map> tickets = embeddedTickets(asAdmin().getForEntity("/api/tickets?size=100", Map.class));

		Map<Number, Map> byId = new java.util.HashMap<>();
		tickets.forEach(ticket -> byId.put(((Number) ticket.get("id")).longValue(), ticket));
		assertThat(byId.keySet()).contains(incidentId.longValue(), problemId.longValue(), changeId.longValue(),
				serviceRequestId.longValue(), foreignIncidentId.longValue());

		assertTypeAndPrefix(byId.get(incidentId.longValue()), "INCIDENT", "INC-");
		assertTypeAndPrefix(byId.get(problemId.longValue()), "PROBLEM", "PRB-");
		assertTypeAndPrefix(byId.get(changeId.longValue()), "CHANGE", "RFC-");
		assertTypeAndPrefix(byId.get(serviceRequestId.longValue()), "SERVICE_REQUEST", "REQ-");
	}

	@Test
	void fetchesASingleTicketWithItsSubtypeLink() {
		ResponseEntity<Map> response = asAdmin().getForEntity("/api/tickets/" + incidentId, Map.class);
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(response.getBody().get("type")).isEqualTo("INCIDENT");
		assertThat((String) response.getBody().get("displayNumber")).startsWith("INC-");

		Map<String, Object> links = (Map<String, Object>) response.getBody().get("_links");
		assertThat(links).containsKeys("self", "incident", "comments");
		assertThat(((Map<String, Object>) links.get("incident")).get("href").toString())
				.endsWith("/api/incidents/" + incidentId);
	}

	@Test
	void filtersByRequesterAcrossSubtypes() {
		List<Map> tickets = embeddedTickets(
				asAdmin().getForEntity("/api/tickets?size=100&requesterId=" + otherCustomerId, Map.class));
		assertThat(tickets).extracting(ticket -> ((Number) ticket.get("requesterId")).longValue())
				.containsOnly(otherCustomerId.longValue());
		assertThat(tickets).extracting(ticket -> ((Number) ticket.get("id")).longValue())
				.contains(foreignIncidentId.longValue());
	}

	@Test
	void customersOnlySeeTheirOwnTicketsAcrossSubtypes() {
		List<Map> tickets = embeddedTickets(asCustomer().getForEntity("/api/tickets?size=100", Map.class));
		assertThat(tickets).extracting(ticket -> ((Number) ticket.get("requesterId")).longValue())
				.containsOnly(customerId.longValue());

		assertThat(asCustomer().getForEntity("/api/tickets/" + foreignIncidentId, String.class).getStatusCode())
				.isEqualTo(HttpStatus.NOT_FOUND);
		assertThat(asCustomer().getForEntity("/api/tickets/" + incidentId, Map.class).getStatusCode())
				.isEqualTo(HttpStatus.OK);
	}

	@Test
	void returnsNotFoundForAMissingTicket() {
		assertThat(asAdmin().getForEntity("/api/tickets/999999", String.class).getStatusCode())
				.isEqualTo(HttpStatus.NOT_FOUND);
	}

	@Test
	void requestsWithoutCredentialsAreRejected() {
		assertThat(restTemplate.getForEntity("/api/tickets", String.class).getStatusCode())
				.isEqualTo(HttpStatus.UNAUTHORIZED);
	}

	private void assertTypeAndPrefix(Map ticket, String expectedType, String expectedPrefix) {
		assertThat(ticket.get("type")).isEqualTo(expectedType);
		assertThat((String) ticket.get("displayNumber")).startsWith(expectedPrefix);
	}

	private Number createCustomer(String name, String email, String username, String password) {
		Map<String, Object> request = Map.of("role", "CUSTOMER", "name", name, "email", email, "username", username,
				"password", password);
		return (Number) asAdmin().postForEntity("/api/persons", request, Map.class).getBody().get("id");
	}

	private Number createTicket(String basePath, String subject, Number requesterId) {
		Map<String, Object> body = Map.of("subject", subject, "requesterId", requesterId);
		return (Number) asAdmin().postForEntity(basePath, body, Map.class).getBody().get("id");
	}

	private TestRestTemplate asAdmin() {
		return restTemplate.withBasicAuth("admin", "admin123");
	}

	private TestRestTemplate asCustomer() {
		return restTemplate.withBasicAuth("carla", "carla12345");
	}

	@SuppressWarnings("unchecked")
	private List<Map> embeddedTickets(ResponseEntity<Map> response) {
		Map<String, Object> embedded = (Map<String, Object>) response.getBody().get("_embedded");
		assertThat(embedded).isNotNull();
		return (List<Map>) embedded.get("ticketModelList");
	}
}
