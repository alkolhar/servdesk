package dev.alkolhar.servdesk.ticket;

import org.jspecify.annotations.Nullable;

/**
 * The optional body of a ticket action: a comment, required for some actions.
 */
public record ActionRequest(@Nullable String comment) {
}
