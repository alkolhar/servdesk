package dev.alkolhar.servdesk.setup;

import dev.alkolhar.servdesk.directory.PasswordPolicy;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.jspecify.annotations.Nullable;

public record SetupRequest(@NotBlank String name, @NotBlank @Email String email, @Nullable String phone,
		@NotBlank String username, @NotBlank @PasswordPolicy String password) {
}
