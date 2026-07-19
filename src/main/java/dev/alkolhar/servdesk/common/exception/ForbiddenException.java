package dev.alkolhar.servdesk.common.exception;

/**
 * Thrown by a command service when the request is well-formed and the target
 * aggregate exists, but the authenticated caller isn't allowed to perform this
 * specific mutation given data in the request body — e.g. a Customer trying to
 * mark their own {@code Comment} {@code internal}. Distinct from a static
 * URL+role rule (already handled by {@code SecurityConfig}'s
 * {@code authorizeHttpRequests}): this is a data-dependent check the service
 * layer has to make, so it can't live there. See {@link NotFoundException} for
 * why this stays free of HTTP concepts.
 */
public class ForbiddenException extends RuntimeException {

	public ForbiddenException(String message) {
		super(message);
	}
}
