package dev.alkolhar.servdesk.customfield;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.jspecify.annotations.Nullable;

public record AttributeDefinitionCreateRequest(@NotNull AttributeTarget target,
		@NotBlank @Pattern(regexp = "[a-z][a-z0-9_]*", message = "key must be a machine name: [a-z][a-z0-9_]*") String key,
		@NotBlank String label, @NotNull AttributeType type, boolean required, @Nullable List<String> enumValues) {
}
