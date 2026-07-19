package dev.alkolhar.servdesk.classification;

import jakarta.validation.constraints.NotBlank;

public record PriorityCreateRequest(@NotBlank String name, int sortOrder) {
}
