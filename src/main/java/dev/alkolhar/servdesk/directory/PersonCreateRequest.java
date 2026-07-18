package dev.alkolhar.servdesk.directory;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

public record PersonCreateRequest(@NotNull PersonRole role, @NotBlank String name, @NotBlank @Email String email,
		@Nullable String phone, @Nullable String username, @Nullable String password, @Nullable Long teamId) {
}
