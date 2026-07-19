package dev.alkolhar.servdesk.ticket;

import org.jspecify.annotations.Nullable;

/**
 * The fields shared by every ticket subtype's create request. Implemented by
 * each subtype's own request record (e.g. {@code IncidentCreateRequest}) so
 * {@link AbstractTicketSubtypeCommandService} can build the shared
 * {@link Ticket} row once, instead of every subtype command service
 * re-implementing the same field-by-field copy.
 */
public interface TicketCreateFields {

	String subject();

	@Nullable String description();

	@Nullable Long categoryId();

	@Nullable Long priorityId();

	Long requesterId();

	@Nullable Long assigneeId();

	@Nullable Long teamId();
}
