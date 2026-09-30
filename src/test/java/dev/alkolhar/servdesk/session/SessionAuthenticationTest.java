package dev.alkolhar.servdesk.session;

import static org.assertj.core.api.Assertions.assertThat;

import dev.alkolhar.servdesk.TestcontainersConfiguration;
import dev.alkolhar.servdesk.setup.SetupRequest;
import java.net.HttpCookie;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Browser sessions alongside HTTP Basic (ADR-0005), through the real filter
 * chain: login, the session cookie's attributes, {@code /api/me}, CSRF on
 * session writes but not on Basic ones, logout, and the 401 that must never
 * trigger the browser's native Basic dialog. Cookies are carried by hand —
 * {@link TestRestTemplate} keeps none — which also keeps each step's
 * credentials explicit.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SessionAuthenticationTest {

	@Autowired
	private TestRestTemplate restTemplate;

	@BeforeAll
	void bootstrapAgent() {
		restTemplate.postForEntity("/api/setup",
				new SetupRequest("Ada Admin", "ada@example.com", null, "admin", "admin-password"), String.class);
	}

	/** The two cookies a successful login leaves the browser holding. */
	private record Session(String sessionId, String xsrfToken) {

		HttpHeaders cookies() {
			HttpHeaders headers = new HttpHeaders();
			headers.add(HttpHeaders.COOKIE, "JSESSIONID=" + sessionId + "; XSRF-TOKEN=" + xsrfToken);
			return headers;
		}

		HttpHeaders cookiesAndCsrfHeader() {
			HttpHeaders headers = cookies();
			headers.add("X-XSRF-TOKEN", xsrfToken);
			return headers;
		}

	}

	private ResponseEntity<String> login(String username, String password) {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		return restTemplate.postForEntity("/api/login",
				new HttpEntity<>(Map.of("username", username, "password", password), headers), String.class);
	}

	private Session loginAsAdmin() {
		ResponseEntity<String> response = login("admin", "admin-password");
		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		return new Session(cookieValue(response, "JSESSIONID"), cookieValue(response, "XSRF-TOKEN"));
	}

	private static String cookieValue(ResponseEntity<?> response, String name) {
		return setCookieHeaders(response).stream().flatMap(header -> HttpCookie.parse(header).stream())
				.filter(cookie -> cookie.getName().equals(name)).map(HttpCookie::getValue).findFirst()
				.orElseThrow(() -> new AssertionError("no " + name + " cookie in " + setCookieHeaders(response)));
	}

	private static List<String> setCookieHeaders(ResponseEntity<?> response) {
		List<String> headers = response.getHeaders().get(HttpHeaders.SET_COOKIE);
		return headers == null ? List.of() : headers;
	}

	private ResponseEntity<String> createCategory(String name, HttpHeaders headers) {
		headers.setContentType(MediaType.APPLICATION_JSON);
		return restTemplate.exchange("/api/categories", HttpMethod.POST,
				new HttpEntity<>(Map.of("name", name), headers), String.class);
	}

	@Test
	void loginSetsAnHttpOnlySameSiteStrictSessionCookie() {
		ResponseEntity<String> response = login("admin", "admin-password");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
		assertThat(setCookieHeaders(response)).anySatisfy(header -> assertThat(header).startsWith("JSESSIONID=")
				.contains("HttpOnly").containsIgnoringCase("SameSite=Strict"));
	}

	@Test
	void theSessionAuthenticatesMe() {
		Session session = loginAsAdmin();

		ResponseEntity<Map> me = restTemplate.exchange("/api/me", HttpMethod.GET, new HttpEntity<>(session.cookies()),
				Map.class);

		assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(me.getBody()).containsEntry("name", "Ada Admin").containsEntry("role", "AGENT")
				.containsEntry("username", "admin");
	}

	@Test
	void basicAuthenticatesMeToo() {
		ResponseEntity<Map> me = restTemplate.withBasicAuth("admin", "admin-password").getForEntity("/api/me",
				Map.class);

		assertThat(me.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(me.getBody()).containsEntry("name", "Ada Admin");
	}

	@Test
	void aWrongPasswordIsA401ProblemWithoutABasicChallenge() {
		ResponseEntity<String> response = login("admin", "wrong");

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(response.getHeaders().getContentType().isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.isTrue();
		assertThat(response.getHeaders().containsHeader(HttpHeaders.WWW_AUTHENTICATE)).isFalse();
		assertThat(setCookieHeaders(response)).noneMatch(header -> header.startsWith("JSESSIONID="));
	}

	@Test
	void anUnauthenticatedMeIsA401WithoutABasicChallenge() {
		ResponseEntity<String> response = restTemplate.getForEntity("/api/me", String.class);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
		assertThat(response.getHeaders().containsHeader(HttpHeaders.WWW_AUTHENTICATE)).isFalse();
		// the request cache is off: being turned away creates no session
		assertThat(setCookieHeaders(response)).noneMatch(header -> header.startsWith("JSESSIONID="));
	}

	@Test
	void aSessionWriteWithoutTheCsrfTokenIsRejected() {
		Session session = loginAsAdmin();

		ResponseEntity<String> response = createCategory("Without token", session.cookies());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
		assertThat(response.getHeaders().getContentType().isCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
				.isTrue();
	}

	@Test
	void aSessionWriteWithTheCsrfTokenSucceeds() {
		Session session = loginAsAdmin();

		ResponseEntity<String> response = createCategory("With token", session.cookiesAndCsrfHeader());

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
	}

	@Test
	void theSameWriteWithBasicNeedsNoCsrfToken() {
		HttpHeaders basic = new HttpHeaders();
		basic.setBasicAuth("admin", "admin-password");

		ResponseEntity<String> response = createCategory("Basic", basic);

		assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
		// Basic stays stateless
		assertThat(setCookieHeaders(response)).noneMatch(header -> header.startsWith("JSESSIONID="));
	}

	@Test
	void logoutEndsTheSession() {
		Session session = loginAsAdmin();

		ResponseEntity<String> withoutToken = restTemplate.exchange("/api/logout", HttpMethod.POST,
				new HttpEntity<>(session.cookies()), String.class);
		assertThat(withoutToken.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);

		ResponseEntity<String> logout = restTemplate.exchange("/api/logout", HttpMethod.POST,
				new HttpEntity<>(session.cookiesAndCsrfHeader()), String.class);
		assertThat(logout.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

		ResponseEntity<String> me = restTemplate.exchange("/api/me", HttpMethod.GET,
				new HttpEntity<>(session.cookies()), String.class);
		assertThat(me.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
	}

}
