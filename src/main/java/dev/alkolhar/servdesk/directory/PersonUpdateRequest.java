package dev.alkolhar.servdesk.directory;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

/**
 * The three login-bearing fields — {@code username}, {@code password},
 * {@code enabled} — all read null as "leave unchanged", deliberately departing
 * from PUT's replace-everything semantics. Omitting one must never revoke a
 * login: that is issue #66, where a generated PUT that simply didn't mention
 * {@code username} erased it and locked the only agent out of a running
 * deployment, 200 OK and all. Revoking access is an act, not an omission.
 */
public record PersonUpdateRequest(@NotNull PersonRole role, @NotBlank String name, @NotBlank @Email String email,
		@Nullable String phone,
		// Null leaves the existing username unchanged
		@Nullable String username,
		// Null leaves the existing password unchanged
		@Nullable String password,
		// Null leaves the existing enabled flag unchanged (a primitive boolean here
		// would default an omitted field to false and silently disable the account)
		@Nullable Boolean enabled, @Nullable Long teamId) {
}
