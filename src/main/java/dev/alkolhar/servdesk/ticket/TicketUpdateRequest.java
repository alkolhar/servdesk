package dev.alkolhar.servdesk.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import org.jspecify.annotations.Nullable;

public record TicketUpdateRequest(@NotNull TicketType type, @NotNull TicketStatus status, @NotBlank String subject,
		@Nullable String description, @Nullable Long categoryId, @Nullable Long priorityId, @NotNull Long requesterId,
		@Nullable Long assigneeId, @Nullable Long teamId, @Nullable Instant resolvedAt, @Nullable Instant closedAt) {
}
