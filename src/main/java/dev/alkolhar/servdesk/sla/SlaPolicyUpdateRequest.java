package dev.alkolhar.servdesk.sla;

import jakarta.validation.constraints.Positive;
import org.jspecify.annotations.Nullable;

/**
 * No {@code priorityId}: which priority a policy governs is fixed at creation —
 * retargeting a policy would silently rewrite the SLA meaning of existing
 * deadlines. Delete and recreate instead.
 */
public record SlaPolicyUpdateRequest(@Positive @Nullable Integer responseMinutes,
		@Positive @Nullable Integer resolutionMinutes) {
}
