package dev.alkolhar.servdesk.ticket.change;

import dev.alkolhar.servdesk.ticket.TicketCreateFields;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import org.jspecify.annotations.Nullable;

public record ChangeCreateRequest(@NotBlank String subject, @Nullable String description, @Nullable Long categoryId,
		@Nullable Long priorityId, @NotNull Long requesterId, @Nullable Long assigneeId, @Nullable Long teamId,
		@Nullable Map<String, Object> attributes) implements TicketCreateFields {
}
