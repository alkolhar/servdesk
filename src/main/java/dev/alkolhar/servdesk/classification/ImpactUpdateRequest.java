package dev.alkolhar.servdesk.classification;

import jakarta.validation.constraints.NotBlank;

public record ImpactUpdateRequest(@NotBlank String name, int sortOrder) {
}
