package dev.alkolhar.servdesk.classification;

import static org.assertj.core.api.Assertions.assertThat;

import dev.alkolhar.servdesk.TestcontainersConfiguration;
import dev.alkolhar.servdesk.setup.SetupRequest;
import java.time.Duration;
import java.time.Instant;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;

/**
 * The starter reference data {@code V2__seed_reference_data.sql} ships (#101):
 * a fresh deployment derives a priority and SLA deadlines from its first ticket
 * with no admin setup at all, and the seeded rows are ordinary rows the admin
 * APIs can edit and soft-delete. Seeded ids are looked up by name through JDBC
 * — which id a row lands on is the database's business, not the seed's.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ReferenceDataSeedTest {

	@Autowired
	private TestRestTemplate restTemplate;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	private Number requesterId;

	@BeforeAll
	void bootstrapFixtures() {
		// only people — no impact, urgency, priority, matrix cell or SLA policy is
		// created here; everything the incident below needs has to come from the seed
		restTemplate.postForEntity("/api/setup",
				new SetupRequest("Administrator", "admin@example.com", null, "admin", "admin123"), String.class);
		Map<String, Object> customerRequest = Map.of("role", "CUSTOMER", "name", "Carla Customer", "email",
				"carla@example.com", "username", "carla", "password", "carla12345");
		requesterId = (Number) asAdmin().postForEntity("/api/persons", customerRequest, Map.class).getBody().get("id");
	}

	private TestRestTemplate asAdmin() {
		return restTemplate.withBasicAuth("admin", "admin123");
	}

	private long seededId(String table, String name) {
		return jdbcTemplate.queryForObject("SELECT id FROM " + table + " WHERE name = ? AND deleted_at IS NULL",
				Long.class, name);
	}

	@Test
	void aFirstIncidentDerivesPriorityAndSlaDeadlinesFromTheSeed() {
		Map<String, Object> body = Map.of("subject", "Mail server down", "requesterId", requesterId, "impactId",
				seededId("impact", "1 - High"), "urgencyId", seededId("urgency", "1 - High"));
		ResponseEntity<Map> created = asAdmin().postForEntity("/api/incidents", body, Map.class);
		assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);

		Map<String, Object> incident = asAdmin()
				.getForEntity("/api/incidents/" + created.getBody().get("id"), Map.class).getBody();
		assertThat(((Number) incident.get("priorityId")).longValue()).isEqualTo(seededId("priority", "P1 - Critical"));
		assertThat(incident.get("respondBy")).isNotNull();
		assertThat(incident.get("resolveBy")).isNotNull();
		// P1's seeded policy: respond within 15 minutes, resolve within 240
		Duration respondToResolve = Duration.between(Instant.parse((String) incident.get("respondBy")),
				Instant.parse((String) incident.get("resolveBy")));
		assertThat(respondToResolve).isEqualTo(Duration.ofMinutes(225));
	}

	@Test
	void theMatrixIsFullyMapped() {
		Integer cells = jdbcTemplate.queryForObject("""
				SELECT count(*) FROM impact i CROSS JOIN urgency u
				WHERE i.deleted_at IS NULL AND u.deleted_at IS NULL
				  AND i.created_by = 'system' AND u.created_by = 'system'
				  AND EXISTS (SELECT 1 FROM priority_definition d
				              WHERE d.impact_id = i.id AND d.urgency_id = u.id AND d.deleted_at IS NULL)
				""", Integer.class);
		assertThat(cells).isEqualTo(9);
		Integer policies = jdbcTemplate.queryForObject("""
				SELECT count(*) FROM priority p
				WHERE p.created_by = 'system'
				  AND EXISTS (SELECT 1 FROM sla_policy s WHERE s.priority_id = p.id AND s.deleted_at IS NULL)
				""", Integer.class);
		assertThat(policies).isEqualTo(4);
	}

	@Test
	void seededRowsAreEditableAndSoftDeletableThroughTheApi() {
		long urgencyId = seededId("urgency", "3 - Low");
		Map<String, Object> rename = new HashMap<>();
		rename.put("name", "3 - Whenever");
		rename.put("sortOrder", 2);
		ResponseEntity<Map> renamed = asAdmin().exchange("/api/urgencies/" + urgencyId, HttpMethod.PUT,
				new HttpEntity<>(rename), Map.class);
		assertThat(renamed.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(renamed.getBody().get("name")).isEqualTo("3 - Whenever");

		long p4PolicyId = jdbcTemplate.queryForObject(
				"SELECT s.id FROM sla_policy s JOIN priority p ON p.id = s.priority_id WHERE p.name = 'P4 - Low'",
				Long.class);
		ResponseEntity<Map> tightened = asAdmin().exchange("/api/sla-policies/" + p4PolicyId, HttpMethod.PUT,
				new HttpEntity<>(Map.of("responseMinutes", 720, "resolutionMinutes", 5760)), Map.class);
		assertThat(tightened.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(tightened.getBody().get("responseMinutes")).isEqualTo(720);

		long categoryId = seededId("category", "Network");
		asAdmin().delete("/api/categories/" + categoryId);
		assertThat(asAdmin().getForEntity("/api/categories/" + categoryId, String.class).getStatusCode())
				.isEqualTo(HttpStatus.NOT_FOUND);
	}

}
