package dev.alkolhar.servdesk.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Deep links into the SPA (ADR-0003): a browser reloading {@code /account} asks
 * the server for a path only the Angular router knows, so a {@code GET} for any
 * such path is forwarded to {@code /index.html} and the router takes it from
 * there.
 * <p>
 * Forwarded: {@code GET}s outside the server's own prefixes whose last path
 * segment has no file extension. Everything else passes through untouched — an
 * unknown {@code /api/...} path still gets its 404 {@code ProblemDetail}, and a
 * missing asset ({@code /main-XYZ.js}, {@code /i18n/fr.json}) stays a 404
 * instead of being answered with HTML.
 * <p>
 * A filter rather than a catch-all {@code @GetMapping}: a path pattern can't
 * say "no dot in the <i>last</i> segment" once {@code /**} is involved, and a
 * controller mapping would outrank the static-resource handler and swallow
 * {@code /i18n/en.json}. Runs after Spring Security's chain, which lets these
 * paths through as public (see {@code SecurityConfig}).
 */
@Component
public class SpaForwardingFilter extends OncePerRequestFilter {

	private static final List<String> SERVER_PREFIXES = List.of("/api", "/docs", "/openapi", "/actuator", "/error",
			"/webjars");

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		if (isSpaRoute(request)) {
			request.getRequestDispatcher("/index.html").forward(request, response);
			return;
		}
		chain.doFilter(request, response);
	}

	static boolean isSpaRoute(HttpServletRequest request) {
		if (!"GET".equals(request.getMethod())) {
			return false;
		}
		String path = request.getRequestURI().substring(request.getContextPath().length());
		if (path.isEmpty() || "/".equals(path) || isUnder(path)) {
			return false;
		}
		String lastSegment = path.substring(path.lastIndexOf('/') + 1);
		return !lastSegment.contains(".");
	}

	private static boolean isUnder(String path) {
		for (String prefix : SERVER_PREFIXES) {
			if (path.equals(prefix) || path.startsWith(prefix + "/")) {
				return true;
			}
		}
		return false;
	}

}
