package dev.alkolhar.servdesk.session;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * JSON login for the SPA (ADR-0005). A controller rather than a form-login
 * filter because the SPA posts JSON, not
 * {@code application/x-www-form-urlencoded} — which is also what makes this
 * endpoint safe without a CSRF token: a cross-site page can only send JSON
 * after a CORS preflight, which servdesk never answers.
 * <p>
 * Unversioned, like {@code SetupController}: session plumbing a client hits
 * before anything else, not a resource.
 * <p>
 * Answers 204 rather than the logged-in person: the SPA starts up from
 * {@code GET /api/me} either way. The response carries the new session cookie
 * and the rotated {@code XSRF-TOKEN}.
 */
@RestController
@RequestMapping("/api/login")
public class LoginController {

	private final AuthenticationManager authenticationManager;
	private final SessionAuthenticationStrategy sessionAuthenticationStrategy;
	private final SecurityContextRepository sessionSecurityContextRepository;
	private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder
			.getContextHolderStrategy();

	public LoginController(AuthenticationManager authenticationManager,
			SessionAuthenticationStrategy sessionAuthenticationStrategy,
			SecurityContextRepository sessionSecurityContextRepository) {
		this.authenticationManager = authenticationManager;
		this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
		this.sessionSecurityContextRepository = sessionSecurityContextRepository;
	}

	@PostMapping
	public ResponseEntity<?> login(@Valid @RequestBody LoginRequest login, HttpServletRequest request,
			HttpServletResponse response) {
		Authentication authentication;
		try {
			authentication = authenticationManager.authenticate(
					UsernamePasswordAuthenticationToken.unauthenticated(login.username(), login.password()));
		} catch (AuthenticationException e) {
			// one answer for a wrong password, an unknown user and a disabled account
			// alike, so a login attempt can't probe which usernames exist
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
					.body(ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, "Invalid username or password."));
		}
		// new session id (fixation protection) and a new CSRF token
		sessionAuthenticationStrategy.onAuthentication(authentication, request, response);
		SecurityContext context = securityContextHolderStrategy.createEmptyContext();
		context.setAuthentication(authentication);
		securityContextHolderStrategy.setContext(context);
		sessionSecurityContextRepository.saveContext(context, request, response);
		return ResponseEntity.noContent().build();
	}

}
