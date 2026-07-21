package dev.alkolhar.servdesk.common.web;

import dev.alkolhar.servdesk.common.exception.ConflictException;
import dev.alkolhar.servdesk.common.exception.ForbiddenException;
import dev.alkolhar.servdesk.common.exception.NotFoundException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.data.core.PropertyReferenceException;
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

	@ExceptionHandler(ForbiddenException.class)
	ProblemDetail handleForbidden(ForbiddenException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, ex.getMessage());
	}

	/**
	 * A concurrent duplicate insert (e.g. two requests racing to create the same
	 * {@code person.email}) can slip past the service layer's own existence check
	 * and hit the database's unique index instead of a clean domain-level
	 * {@link ConflictException}. Soft-deleted rows don't trigger this: the unique
	 * indexes are partial ({@code WHERE deleted_at IS NULL}), so a deleted row's
	 * values are free for reuse. The underlying SQL/constraint name is deliberately
	 * not exposed in the response.
	 */
	@ExceptionHandler(DataIntegrityViolationException.class)
	ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
				"The request conflicts with an existing resource, e.g. a unique value is already in use.");
	}

	/**
	 * A {@code ?sort=} query param naming a field that doesn't exist on the target
	 * entity ({@link PropertyReferenceException}), or one Spring Data JPA refuses
	 * to translate into a safe {@code ORDER BY} clause
	 * ({@link InvalidDataAccessApiUsageException}, e.g. a malformed expression),
	 * reaches Spring Data's resolver unvalidated; without this handler either is an
	 * uncaught 500, but an unrecognized/malformed sort field is a client input
	 * error, not a server fault.
	 */
	@ExceptionHandler({PropertyReferenceException.class, InvalidDataAccessApiUsageException.class})
	ProblemDetail handleInvalidSortProperty(RuntimeException ex) {
		return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
	}
}
