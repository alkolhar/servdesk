package dev.alkolhar.servdesk.config;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.firewall.RequestRejectedHandler;
import tools.jackson.databind.ObjectMapper;

/**
 * Authentication and authorization are deliberately two separate concerns
 * below, kept that way so swapping the authentication mechanism later (see the
 * OAuth2/OIDC note on {@link #filterChain}) never has to touch the RBAC rules,
 * and so authorization stays centralized here rather than scattered across
 * {@code @PreAuthorize} annotations in the service layer — a service method
 * should never need to know which HTTP verb or role reached it.
 */
@Configuration
public class SecurityConfig {

	private final ObjectMapper objectMapper;

	public SecurityConfig(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	/**
	 * CSRF protection guards cookie-authenticated browser sessions. This API is
	 * authenticated via HTTP Basic (credentials sent explicitly on every request),
	 * which isn't vulnerable to CSRF, and with the default config enabled, CSRF
	 * checks run before the Basic Auth filter and reject unauthenticated-looking
	 * POST/PUT/DELETE requests before credentials are even evaluated.
	 * <p>
	 * <b>RBAC</b>: {@link dev.alkolhar.servdesk.directory.PersonUserDetailsService}
	 * already maps {@code Person.role} to a
	 * {@code ROLE_AGENT}/{@code ROLE_CUSTOMER} {@code GrantedAuthority} — the rules
	 * below are the first thing that actually reads it. Policy: the person
	 * directory (creating/editing/deleting agents and customers) is an AGENT-only
	 * concern. Classification lookup data (Category/Priority) can be read by either
	 * role, but only an AGENT can create/update/delete it — same
	 * read-open/write-AGENT-only shape as ticket subtypes, since it's reference
	 * data tickets point to, not sensitive personal data like the person directory.
	 * Ticket subtypes (Incident/Problem/Change/Service Request, see ADR-0001) can
	 * be read by either role, but creating one, changing its status, or deleting it
	 * is AGENT-only for this iteration — a deliberate narrowing from the old flat
	 * {@code Ticket}'s policy, since customer self-service creation is deferred
	 * until finer-grained RBAC and/or a future workflow/process engine exist to
	 * support it properly. Comments ({@code /api/tickets/*}{@code /comments}) can
	 * be read and created by either role — a customer needs to reply on their own
	 * ticket — with the {@code internal}-flag-is-Agent-only rule enforced by
	 * {@code CommentCommandService} instead, since that's a data-dependent check
	 * (the request body's {@code internal} flag together with the caller's role),
	 * not a static URL+role rule. Row-level ownership (a customer seeing only
	 * <i>their own</i> tickets, issue #28) lives in the service layer for the same
	 * reason: it compares the caller's identity against the loaded ticket's
	 * requester — see {@code AbstractTicketSubtypeQueryService.findByIdVisibleTo}
	 * and the requester filter in each subtype repository's {@code findVisible}.
	 * <p>
	 * <b>OAuth2/OIDC migration path</b>: this method is the only place that would
	 * change. Swap {@code .httpBasic(withDefaults())} for
	 * {@code .oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()))}, point
	 * {@code spring.security.oauth2.resourceserver.jwt.issuer-uri} at the IdP, and
	 * replace {@code PersonUserDetailsService} with a
	 * {@code JwtAuthenticationConverter} that maps a claim (e.g. {@code roles}) to
	 * the same {@code ROLE_*} authorities — the {@code authorizeHttpRequests} block
	 * below wouldn't need to change at all.
	 * {@code spring-boot-starter-oauth2-resource-server} is already on the
	 * classpath (see pom.xml) so that conversion is additive, not a rewrite; it's
	 * not wired up as an alternative filter chain yet because there's no IdP in
	 * this environment to verify it against — an inactive, unverified security
	 * config is worse than none. Stand up an IdP (e.g. Keycloak via Docker Compose,
	 * see the deployment phase) before actually flipping this.
	 */
	@Bean
	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
		http.csrf(AbstractHttpConfigurer::disable).authorizeHttpRequests(auth -> auth
				// reachable pre-auth: it's how the first Person gets created; see
				// SetupController
				.requestMatchers("/api/setup/**").permitAll()
				// hand-written OpenAPI contract (/openapi), its Swagger UI viewer page
				// (/docs) and the swagger-ui webjar's static assets (/webjars) —
				// documentation should be discoverable without credentials, same as a
				// public API reference
				.requestMatchers("/openapi/**", "/docs/**", "/webjars/**").permitAll()
				// RestExceptionHandler answers NotFoundException/ConflictException/
				// DataIntegrityViolationException directly as a ProblemDetail body,
				// without ever calling sendError, so those no longer forward here. /error
				// still matters for everything that class doesn't handle: a 404 for a URL
				// with no matching handler at all, or any other uncaught exception —
				// reached via Tomcat's internal forward, which on an unauthenticated
				// request carries no credentials, so without this permitAll entry
				// AuthorizationFilter would reject the forwarded /error request and
				// silently overwrite the real status with 401
				.requestMatchers("/error").permitAll()
				// person directory management is AGENT-only
				.requestMatchers("/api/persons/**").hasRole("AGENT")
				// classification lookup data (Category/Priority), custom-field
				// definitions, and SLA policies: either role can read, only an AGENT can
				// create/update/delete — same reference-data shape
				.requestMatchers(HttpMethod.GET, "/api/categories/**", "/api/priorities/**",
						"/api/attribute-definitions/**", "/api/sla-policies/**")
				.hasAnyRole("AGENT", "CUSTOMER")
				.requestMatchers(HttpMethod.POST, "/api/categories/**", "/api/priorities/**",
						"/api/attribute-definitions/**", "/api/sla-policies/**")
				.hasRole("AGENT")
				.requestMatchers(HttpMethod.PUT, "/api/categories/**", "/api/priorities/**",
						"/api/attribute-definitions/**", "/api/sla-policies/**")
				.hasRole("AGENT")
				.requestMatchers(HttpMethod.DELETE, "/api/categories/**", "/api/priorities/**",
						"/api/attribute-definitions/**", "/api/sla-policies/**")
				.hasRole("AGENT")
				// ticket subtypes: either role can read, only an AGENT can create, change
				// status, or delete (see ADR-0001; a deliberate narrowing from the old flat
				// Ticket's policy — customer self-service creation is deferred)
				.requestMatchers(HttpMethod.GET, "/api/incidents/**", "/api/problems/**", "/api/changes/**",
						"/api/service-requests/**")
				.hasAnyRole("AGENT", "CUSTOMER")
				.requestMatchers(HttpMethod.POST, "/api/incidents/**", "/api/problems/**", "/api/changes/**",
						"/api/service-requests/**")
				.hasRole("AGENT")
				.requestMatchers(HttpMethod.PUT, "/api/incidents/**", "/api/problems/**", "/api/changes/**",
						"/api/service-requests/**")
				.hasRole("AGENT")
				.requestMatchers(HttpMethod.DELETE, "/api/incidents/**", "/api/problems/**", "/api/changes/**",
						"/api/service-requests/**")
				.hasRole("AGENT")
				// comments: either role can read or add one (a customer needs to reply on
				// their own ticket); the internal-flag-is-Agent-only rule is enforced by
				// CommentCommandService, not here (see the class javadoc above)
				.requestMatchers(HttpMethod.GET, "/api/tickets/*/comments").hasAnyRole("AGENT", "CUSTOMER")
				.requestMatchers(HttpMethod.POST, "/api/tickets/*/comments").hasAnyRole("AGENT", "CUSTOMER")
				// cross-subtype ticket overview (issue #30): read-only for either role —
				// there is no write surface under /api/tickets itself, writes stay on the
				// subtype endpoints above; row-level ownership is enforced by
				// TicketQueryService like everywhere else
				.requestMatchers(HttpMethod.GET, "/api/tickets/**").hasAnyRole("AGENT", "CUSTOMER").anyRequest()
				.authenticated())
				.httpBasic(basic -> basic.authenticationEntryPoint(problemDetailAuthenticationEntryPoint()))
				.exceptionHandling(exceptions -> exceptions.accessDeniedHandler(problemDetailAccessDeniedHandler()));
		return http.build();
	}

	/**
	 * Both the 401 (missing/bad credentials) and 403 (authenticated but
	 * unauthorized) paths are decided inside Spring Security's filter chain, before
	 * a request ever reaches {@code DispatcherServlet} — so
	 * {@code RestExceptionHandler} never sees them, and without these two handlers
	 * they'd fall back to Spring Security's own default bodies (not RFC 7807
	 * {@code ProblemDetail}, unlike every other error response this API returns).
	 * {@code withDefaults()} would keep that default body; overriding both here is
	 * what makes the 401/403 shape consistent with the rest of the API.
	 */
	private AuthenticationEntryPoint problemDetailAuthenticationEntryPoint() {
		return (request, response, authException) -> writeProblemDetail(response, HttpStatus.UNAUTHORIZED,
				"Full authentication is required to access this resource.");
	}

	private AccessDeniedHandler problemDetailAccessDeniedHandler() {
		return (request, response, accessDeniedException) -> writeProblemDetail(response, HttpStatus.FORBIDDEN,
				"Access is denied.");
	}

	/**
	 * {@code StrictHttpFirewall} rejects a malformed request (e.g. a control
	 * character in a header value) before it even reaches
	 * {@code authorizeHttpRequests} — Spring Boot auto-detects this bean and wires
	 * it into {@code FilterChainProxy}, same reasoning as the two handlers above.
	 */
	@Bean
	RequestRejectedHandler requestRejectedHandler() {
		return (request, response, requestRejectedException) -> writeProblemDetail(response, HttpStatus.BAD_REQUEST,
				"The request was rejected: " + requestRejectedException.getMessage());
	}

	private void writeProblemDetail(HttpServletResponse response, HttpStatus status, String detail) throws IOException {
		response.setStatus(status.value());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		objectMapper.writeValue(response.getWriter(), ProblemDetail.forStatusAndDetail(status, detail));
	}

	/**
	 * Defining a UserDetailsService bean (PersonUserDetailsService) makes Spring
	 * Boot back off from its default in-memory single-user/generated-password
	 * setup; this encoder is what the resulting auto-wired
	 * DaoAuthenticationProvider uses to verify credentials against Person.password.
	 */
	@Bean
	PasswordEncoder passwordEncoder() {
		return new DelegatingPasswordEncoder("argon2", Map.of("bcrypt", new BCryptPasswordEncoder(), "argon2",
				Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()));
	}

}
