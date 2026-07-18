package dev.alkolhar.servdesk.common.web;

import dev.alkolhar.servdesk.common.exception.ConflictException;
import dev.alkolhar.servdesk.common.exception.NotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates exceptions to RFC 7807 {@code application/problem+json} bodies via
 * {@link ProblemDetail}. {@code /error} stays permitAll in
 * {@code SecurityConfig} regardless: it's still reached by errors this class
 * doesn't handle (e.g. a 404 for a URL with no matching handler at all), and by
 * Spring Boot's own default {@code MethodArgumentNotValidException} handling,
 * which already produces a {@code application/problem+json} body without any
 * code here.
 */
@RestControllerAdvice
public class RestExceptionHandler {

	@ExceptionHandler(NotFoundException.class)
	ProblemDetail handleNotFound(NotFoundException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
	}

	@ExceptionHandler(ConflictException.class)
	ProblemDetail handleConflict(ConflictException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
	}

	/**
	 * Soft delete (see {@code BaseEntity}) leaves a deleted row's unique columns
	 * (e.g. {@code person.email}) still occupying their index, since MariaDB has no
	 * partial/filtered unique index — recreating a row with that value hits this
	 * rather than a clean domain-level {@link ConflictException}. The underlying
	 * SQL/constraint name is deliberately not exposed in the response.
	 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
				"The request conflicts with an existing resource, e.g. a unique value is already in use.");
	}
}
