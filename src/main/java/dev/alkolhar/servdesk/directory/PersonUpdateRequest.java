package dev.alkolhar.servdesk.directory;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

public record PersonUpdateRequest(@NotNull PersonRole role, @NotBlank String name, @NotBlank @Email String email,
		@Nullable String phone, @Nullable String username,
		// Null leaves the existing password unchanged
		@Nullable String password, boolean enabled, @Nullable Long teamId) {
}
