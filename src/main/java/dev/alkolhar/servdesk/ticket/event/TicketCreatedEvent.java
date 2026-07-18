package dev.alkolhar.servdesk.ticket.event;

import dev.alkolhar.servdesk.common.event.DomainEvent;

public record TicketCreatedEvent(Long ticketId, String ticketNumber) implements DomainEvent {
}
