package dev.alkolhar.servdesk.classification;

import jakarta.validation.constraints.NotBlank;

public record UrgencyCreateRequest(@NotBlank String name, int sortOrder) {
}
