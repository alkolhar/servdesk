package dev.alkolhar.servdesk.ticket.event;

import dev.alkolhar.servdesk.common.event.DomainEvent;
import dev.alkolhar.servdesk.ticket.TicketStatus;

/**
 * The intended hook point for future SLA-timer and notification modules —
 * published only when a status update actually changes the status, not on every
 * save. Currently unpublished: the generic {@code TicketCommandService} that
 * used to publish it was removed in the composed-Ticket split (ADR-0001); the
 * shared base command service each subtype's command service extends (built
 * starting with the Incident CRUD issue) is expected to publish it in the same
 * shape once it exists.
 */
public record TicketStatusChangedEvent(Long ticketId, TicketStatus previousStatus,
		TicketStatus newStatus) implements DomainEvent {
}
