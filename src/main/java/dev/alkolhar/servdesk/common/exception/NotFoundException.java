package dev.alkolhar.servdesk.common.exception;

/**
 * Thrown by a query/command service when a requested aggregate does not exist.
 * Kept free of any HTTP concept —
 * {@link dev.alkolhar.servdesk.common.web.RestExceptionHandler} is the only
 * place that translates it to a status code, so the service layer stays
 * reusable outside a web context (e.g. from a future Quartz job or Spring
 * Integration flow).
 */
public class NotFoundException extends RuntimeException {

	public NotFoundException(String message) {
		super(message);
	}
}
