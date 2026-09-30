package dev.alkolhar.servdesk.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Which requests {@link SecurityConfig} holds to a CSRF token (ADR-0005): a
 * write that a live session cookie authenticates, and nothing else. The
 * end-to-end behaviour is covered by {@code SessionAuthenticationTest}; this
 * pins down the rule's edges without a server.
 */
class CsrfScopeTest {

	@ParameterizedTest
	@CsvSource({
			// method, live session?, Authorization header, CSRF required?
			"POST,   true,  ,                       true", "PUT,    true,  ,                       true",
			"DELETE, true,  ,                       true",
			// reads never need a token
			"GET,    true,  ,                       false",
			// a Basic header can't be forged cross-site, with or without a session
			// alongside
			"POST,   false, Basic YWRtaW46YWRtaW4=, false", "POST,   true,  Basic YWRtaW46YWRtaW4=, false",
			"POST,   true,  basic YWRtaW46YWRtaW4=, false",
			// no live session: nothing ambient to abuse (first login, /api/setup, stale
			// cookie)
			"POST,   false, ,                       false",
			// any other scheme doesn't earn the Basic exemption
			"POST,   true,  Bearer abc,             true"})
	void requiresATokenOnlyForSessionAuthenticatedWrites(String method, boolean liveSession, String authorization,
			boolean expected) {
		MockHttpServletRequest request = new MockHttpServletRequest(method.trim(), "/api/categories");
		request.setRequestedSessionIdValid(liveSession);
		if (authorization != null) {
			request.addHeader("Authorization", authorization.trim());
		}

		assertThat(SecurityConfig.requiresCsrfProtection(request)).isEqualTo(expected);
	}

}
