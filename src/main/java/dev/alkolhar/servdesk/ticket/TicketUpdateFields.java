package dev.alkolhar.servdesk.ticket;

/**
 * {@link TicketCreateFields} plus {@code status} — the only additional shared
 * field an update request carries over a create request.
 * {@code resolvedAt}/{@code closedAt} are deliberately absent: they're
 * server-derived from a status transition by
 * {@link AbstractTicketSubtypeCommandService}, never client-supplied.
 */
public interface TicketUpdateFields extends TicketCreateFields {

	TicketStatus status();
}
