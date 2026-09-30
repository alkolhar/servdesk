package dev.alkolhar.servdesk.directory;

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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Changing your own password (#102) and the password policy (#89) through the
 * real stack. Each test works on its own person, so no test depends on another
 * having changed — or not changed — a password first.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PasswordChangeTest {

	@Autowired
	private TestRestTemplate restTemplate;

	private int people;

	@BeforeAll
	void bootstrapAdmin() {
		restTemplate.postForEntity("/api/setup",
				new SetupRequest("Administrator", "admin@example.com", null, "admin", "admin-password"), String.class);
	}

	private TestRestTemplate asAdmin() {
		return restTemplate.withBasicAuth("admin", "admin-password");
	}

	/** A fresh Agent with {@code password}; returns their id. */
	private Number agent(String username, String password) {
		people++;
		Map<String, Object> request = Map.of("role", "AGENT", "name", "Agent " + people, "email",
				"agent" + people + "@example.com", "username", username, "password", password);
		ResponseEntity<Map> created = asAdmin().postForEntity("/api/persons", request, Map.class);
		assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		return (Number) created.getBody().get("id");
	}

	private ResponseEntity<String> changePassword(String username, String password, String current, String next) {
		Map<String, Object> body = new HashMap<>();
		body.put("currentPassword", current);
		body.put("newPassword", next);
		return restTemplate.withBasicAuth(username, password).exchange("/api/me/password", HttpMethod.PUT,
				new HttpEntity<>(body), String.class);
	}

	private boolean canLogIn(String username, String password) {
		return restTemplate.withBasicAuth(username, password).getForEntity("/api/me", String.class)
				.getStatusCode() == HttpStatus.OK;
	}

	@Test
	void theNewPasswordLogsInAndTheOldOneNoLongerDoes() {
		agent("grace", "grace-password");

		ResponseEntity<String> response = changePassword("grace", "grace-password", "grace-password",
				"a-brand-new-password");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		assertThat(canLogIn("grace", "a-brand-new-password")).isTrue();
		assertThat(canLogIn("grace", "grace-password")).isFalse();
	}

	@Test
	void aWrongCurrentPasswordIsRefusedAndChangesNothing() {
		agent("hedy", "hedy-password");

		ResponseEntity<String> response = changePassword("hedy", "hedy-password", "not-my-password",
				"a-brand-new-password");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		assertThat(response.getHeaders().getContentType().isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.isTrue();
		assertThat(canLogIn("hedy", "hedy-password")).isTrue();
		assertThat(canLogIn("hedy", "a-brand-new-password")).isFalse();
	}

	@Test
	void aNewPasswordOutsideTwelveToSeventyTwoCharactersIsA400() {
		agent("katherine", "katherine-password");

		for (String tooShortOrTooLong : new String[]{"eleven-char", "x".repeat(73)}) {
			ResponseEntity<String> response = changePassword("katherine", "katherine-password", "katherine-password",
					tooShortOrTooLong);

			assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
			assertThat(response.getHeaders().getContentType().isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
					.isTrue();
		}
		assertThat(canLogIn("katherine", "katherine-password")).isTrue();
	}

	@Test
	void theBoundariesThemselvesAreAccepted() {
		agent("mary", "mary-password");

		assertThat(changePassword("mary", "mary-password", "mary-password", "x".repeat(12)).getStatusCode())
				.isEqualTo(HttpStatus.NO_CONTENT);
		assertThat(changePassword("mary", "x".repeat(12), "x".repeat(12), "y".repeat(72)).getStatusCode())
				.isEqualTo(HttpStatus.NO_CONTENT);
	}

	@Test
	void creatingAPersonWithAShortPasswordIsA400() {
		Map<String, Object> request = Map.of("role", "AGENT", "name", "Shorty", "email", "shorty@example.com",
				"username", "shorty", "password", "too-short");

		ResponseEntity<String> response = asAdmin().postForEntity("/api/persons", request, String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
	}

	/**
	 * {@code password} is gone from the update request: sent anyway, it's an
	 * unknown property and ignored, so the one door for a credential change stays
	 * the one door.
	 */
	@Test
	void updatingAPersonCanNoLongerChangeTheirPassword() {
		Number id = agent("dorothy", "dorothy-password");
		Map<String, Object> update = new HashMap<>();
		update.put("role", "AGENT");
		update.put("name", "Dorothy Vaughan");
		update.put("email", "dorothy@example.com");
		update.put("password", "sneaky-new-password");

		ResponseEntity<String> response = asAdmin().exchange("/api/persons/" + id, HttpMethod.PUT,
				new HttpEntity<>(update), String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(canLogIn("dorothy", "dorothy-password")).isTrue();
		assertThat(canLogIn("dorothy", "sneaky-new-password")).isFalse();
	}

}
