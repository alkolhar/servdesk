package dev.alkolhar.servdesk.directory;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

/**
 * The login-bearing fields left here — {@code username}, {@code enabled} — read
 * null as "leave unchanged", deliberately departing from PUT's
 * replace-everything semantics. Omitting one must never revoke a login: that is
 * issue #66, where a generated PUT that simply didn't mention {@code username}
 * erased it and locked the only agent out of a running deployment, 200 OK and
 * all. Revoking access is an act, not an omission.
 * <p>
 * No {@code password} (#89): a credential change gets exactly one door each —
 * {@code PUT /api/me/password} for your own, an admin reset for someone else's.
 */
public record PersonUpdateRequest(@NotNull PersonRole role, @NotBlank String name, @NotBlank @Email String email,
		@Nullable String phone,
		// Null leaves the existing username unchanged
		@Nullable String username,
		// Null leaves the existing enabled flag unchanged (a primitive boolean here
		// would default an omitted field to false and silently disable the account)
		@Nullable Boolean enabled, @Nullable Long teamId) {
}
