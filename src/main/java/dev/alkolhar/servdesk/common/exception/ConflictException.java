package dev.alkolhar.servdesk.common.exception;

/**
 * Thrown by a command service when a request conflicts with the current state
 * of an aggregate (e.g. setup already completed). See {@link NotFoundException}
 * for why this stays free of HTTP concepts.
 */
public class ConflictException extends RuntimeException {

	public ConflictException(String message) {
		super(message);
	}
}
