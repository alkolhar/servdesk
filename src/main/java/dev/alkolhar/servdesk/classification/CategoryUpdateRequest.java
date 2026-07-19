package dev.alkolhar.servdesk.classification;

import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.Nullable;

public record CategoryUpdateRequest(@NotBlank String name, @Nullable Long parentId) {
}
