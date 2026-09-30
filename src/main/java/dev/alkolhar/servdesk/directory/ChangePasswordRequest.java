package dev.alkolhar.servdesk.directory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * {@code PUT /api/me/password}. {@code currentPassword} is only compared, so
 * the policy doesn't apply to it (see {@link PasswordPolicy}).
 */
public record ChangePasswordRequest(@NotBlank String currentPassword, @NotNull @PasswordPolicy String newPassword) {
}
