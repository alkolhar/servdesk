package dev.alkolhar.servdesk.classification;

import jakarta.validation.constraints.NotBlank;

public record ImpactCreateRequest(@NotBlank String name, int sortOrder) {
}
