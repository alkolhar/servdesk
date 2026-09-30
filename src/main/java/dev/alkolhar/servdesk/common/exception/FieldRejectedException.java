package dev.alkolhar.servdesk.common.exception;

/**
 * Unusable input that belongs to one field of the request — still an
 * {@link IllegalArgumentException} (400), but carrying the field's path in the
 * request body ({@code attributes.costCentre}) and a stable code, so the 400
 * can point a form at the right field. {@code RestExceptionHandler} reports it
 * in the ProblemDetail's {@code errors} list, the same shape Bean Validation
 * failures use. HTTP-agnostic, like the other exceptions here.
 */
public class FieldRejectedException extends IllegalArgumentException {

	private final String field;
	private final String code;

	public FieldRejectedException(String field, String code, String message) {
		super(message);
		this.field = field;
		this.code = code;
	}

	public String getField() {
		return field;
	}

	public String getCode() {
		return code;
	}

}
