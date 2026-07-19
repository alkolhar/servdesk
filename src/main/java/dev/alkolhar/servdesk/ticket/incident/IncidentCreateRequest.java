package dev.alkolhar.servdesk.ticket.incident;

import dev.alkolhar.servdesk.ticket.TicketCreateFields;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.jspecify.annotations.Nullable;

public record IncidentCreateRequest(@NotBlank String subject, @Nullable String description, @Nullable Long categoryId,
		@Nullable Long impactId, @Nullable Long urgencyId, @NotNull Long requesterId, @Nullable Long assigneeId,
		@Nullable Long teamId, @Nullable Long relatedProblemId) implements TicketCreateFields {
}
