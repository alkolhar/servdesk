package dev.alkolhar.servdesk.setup;

import static org.assertj.core.api.Assertions.assertThat;

import dev.alkolhar.servdesk.TestcontainersConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Exercises the one-shot nature of the setup flow, so it needs an isolated
 * database with no Person rows to start from — {@code @DirtiesContext}
 * guarantees this class gets its own Testcontainers instance rather than
 * sharing (and racing on) one with other test classes.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SetupControllerTest {

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void firstPersonMustBeCreatedThroughSetupBeforeTheApiIsUsable() {
		ResponseEntity<SetupStatus> before = restTemplate.getForEntity("/api/setup", SetupStatus.class);
		assertThat(before.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(before.getBody().setupRequired()).isTrue();

		// nothing in the API works yet: there is no Person to authenticate as
		ResponseEntity<String> beforeSetup = restTemplate.getForEntity("/api/persons", String.class);
		assertThat(beforeSetup.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

		SetupRequest request = new SetupRequest("Administrator", "admin@example.com", null, "admin", "admin123");
		ResponseEntity<String> created = restTemplate.postForEntity("/api/setup", request, String.class);
		assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		assertThat(created.getBody()).doesNotContain("password").contains("\"admin\"");

		ResponseEntity<SetupStatus> after = restTemplate.getForEntity("/api/setup", SetupStatus.class);
		assertThat(after.getBody().setupRequired()).isFalse();

		// the created agent can now authenticate against the rest of the API
		ResponseEntity<String> authed = restTemplate.withBasicAuth("admin", "admin123").getForEntity("/api/persons",
				String.class);
		assertThat(authed.getStatusCode()).isEqualTo(HttpStatus.OK);

		// setup can't be used to mint a second account once the first exists
		ResponseEntity<String> secondAttempt = restTemplate.postForEntity("/api/setup", request, String.class);
		assertThat(secondAttempt.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
	}
}
