package dev.alkolhar.servdesk.ticket.incident;

import dev.alkolhar.servdesk.ticket.TicketCreateFields;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import org.jspecify.annotations.Nullable;

/**
 * No {@code status}: an Incident's status is its lifecycle process's projection
 * (ADR-0004) and moves only through the process. Sent anyway, it's an unknown
 * property and ignored.
 */
public record IncidentUpdateRequest(@NotBlank String subject, @Nullable String description, @Nullable Long categoryId,
		@Nullable Long impactId, @Nullable Long urgencyId, @NotNull Long requesterId, @Nullable Long assigneeId,
		@Nullable Long teamId, @Nullable Long relatedProblemId,
		@Nullable Map<String, Object> attributes) implements TicketCreateFields {
}
