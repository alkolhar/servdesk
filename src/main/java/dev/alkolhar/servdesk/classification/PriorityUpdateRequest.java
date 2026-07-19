package dev.alkolhar.servdesk.classification;

import jakarta.validation.constraints.NotBlank;

public record PriorityUpdateRequest(@NotBlank String name, int sortOrder) {
}
