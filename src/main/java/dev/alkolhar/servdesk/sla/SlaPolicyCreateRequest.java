package dev.alkolhar.servdesk.sla;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.jspecify.annotations.Nullable;

public record SlaPolicyCreateRequest(@NotNull Long priorityId, @Positive @Nullable Integer responseMinutes,
		@Positive @Nullable Integer resolutionMinutes) {
}
