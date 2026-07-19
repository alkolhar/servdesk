package dev.alkolhar.servdesk.classification;

import jakarta.validation.constraints.NotNull;

public record PriorityDefinitionCreateRequest(@NotNull Long impactId, @NotNull Long urgencyId,
		@NotNull Long priorityId) {
}
