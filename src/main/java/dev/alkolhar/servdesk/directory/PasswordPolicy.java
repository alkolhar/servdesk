package dev.alkolhar.servdesk.directory;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import jakarta.validation.ReportAsSingleViolation;
import jakarta.validation.constraints.Size;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * The password policy (#89), on every password a request <i>sets</i>: person
 * create, first-run setup, changing your own. 12 to 72 characters and nothing
 * else — no composition rules, per current NIST guidance; 72 is BCrypt's input
 * limit, kept so the encoder can change without the policy changing. A password
 * that is only being <i>checked</i> (the current one, on a change) is never
 * held to it, so an account from before the policy can still move off its old
 * password. Null passes: whether a password is required is each request's own
 * {@code @NotNull}/{@code @NotBlank}.
 */
@Documented
@Constraint(validatedBy = {})
@ReportAsSingleViolation
@Size(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.MAX_LENGTH) @Target({FIELD, METHOD, PARAMETER})
@Retention(RUNTIME)
public @interface PasswordPolicy {

	int MIN_LENGTH = 12;

	int MAX_LENGTH = 72;

	String message() default "must be between " + MIN_LENGTH + " and " + MAX_LENGTH + " characters";

	Class<?>[] groups() default {};

	Class<? extends Payload>[] payload() default {};

}
