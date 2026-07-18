package dev.alkolhar.servdesk.ticket.event;

import dev.alkolhar.servdesk.common.event.DomainEvent;
import dev.alkolhar.servdesk.ticket.TicketStatus;

/**
 * The intended hook point for future SLA-timer and notification modules —
 * published only when {@code update()} actually changes the status, not on
 * every save.
 */
public record TicketStatusChangedEvent(Long ticketId, TicketStatus previousStatus,
		TicketStatus newStatus) implements DomainEvent {
}
