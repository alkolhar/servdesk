package dev.alkolhar.servdesk.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class SpaForwardingFilterTest {

	@ParameterizedTest
	@CsvSource({"GET, /account, /index.html", "GET, /tickets/42, /index.html", "GET, /queues/my-work, /index.html",
			// the welcome page already serves index.html at the root
			"GET, /, ",
			// the server's own prefixes keep their real answers (a 404 ProblemDetail, the
			// docs)
			"GET, /api/nope, ", "GET, /api, ", "GET, /docs/index.html, ", "GET, /openapi/servdesk-api.yaml, ",
			"GET, /actuator/health, ", "GET, /error, ", "GET, /webjars/swagger-ui/index.css, ",
			// a missing asset stays a 404, not HTML
			"GET, /main-ABC123.js, ", "GET, /i18n/fr.json, ",
			// only reads navigate
			"POST, /account, "})
	void forwardsOnlySpaRoutesToIndexHtml(String method, String path, String expectedForward) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest(method, path);
		MockHttpServletResponse response = new MockHttpServletResponse();
		MockFilterChain chain = new MockFilterChain();

		new SpaForwardingFilter().doFilter(request, response, chain);

		assertThat(response.getForwardedUrl()).isEqualTo(expectedForward);
		// a forwarded request never reaches the rest of the chain; a passed-through one
		// does
		assertThat(chain.getRequest()).isEqualTo(expectedForward == null ? request : null);
	}

}
