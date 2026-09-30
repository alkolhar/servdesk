package dev.alkolhar.servdesk.customfield;

import dev.alkolhar.servdesk.common.exception.FieldRejectedException;
import java.util.Locale;

/**
 * A custom-field value the definitions don't allow, reported against the
 * request's {@code attributes.<key>} field so a form can show it next to that
 * field.
 */
public class InvalidAttributeException extends FieldRejectedException {

	/**
	 * Why a value was refused; its lower-case name is the error code clients see.
	 */
	public enum Reason {
		/** No definition exists for the key. */
		UNKNOWN,
		/** A required attribute is missing. */
		REQUIRED,
		/** The value isn't of the definition's type. */
		TYPE,
		/** An ENUM value outside the defined set. */
		NOT_ALLOWED
	}

	public InvalidAttributeException(String key, Reason reason, String message) {
		super("attributes." + key, reason.name().toLowerCase(Locale.ROOT), message);
	}

}
