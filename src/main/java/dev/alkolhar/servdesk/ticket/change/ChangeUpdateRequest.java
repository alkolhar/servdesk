package dev.alkolhar.servdesk.ticket.change;

import dev.alkolhar.servdesk.ticket.TicketStatus;
import dev.alkolhar.servdesk.ticket.TicketUpdateFields;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

public record ChangeUpdateRequest(@NotNull TicketStatus status, @NotBlank String subject, @Nullable String description,
		@Nullable Long categoryId, @Nullable Long impactId, @Nullable Long urgencyId, @NotNull Long requesterId,
		@Nullable Long assigneeId, @Nullable Long teamId) implements TicketUpdateFields {
}
