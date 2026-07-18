package dev.alkolhar.servdesk.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

public record TicketCreateRequest(@NotNull TicketType type, @NotBlank String subject, @Nullable String description,
		@Nullable Long categoryId, @Nullable Long priorityId, @NotNull Long requesterId, @Nullable Long assigneeId,
		@Nullable Long teamId) {
}
