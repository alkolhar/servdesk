package dev.alkolhar.servdesk.customfield;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Deliberately carries no {@code target}/{@code key}/{@code type}: those are
 * immutable after creation (changing them would silently invalidate values
 * already stored on tickets) — see
 * {@code AttributeDefinitionCommandService#update}.
 */
public record AttributeDefinitionUpdateRequest(@NotBlank String label, boolean required,
		@Nullable List<String> enumValues) {
}
