package dev.alkolhar.servdesk.ticket.overview;

import dev.alkolhar.servdesk.ticket.Ticket;

/**
 * A shared {@link Ticket} row paired with the subtype facts
 * ({@link TicketType}, display number) that live on the subtype's own table —
 * resolved by {@link TicketQueryService}, consumed by
 * {@link TicketModelAssembler}.
 */
public record TicketOverview(Ticket ticket, TicketType type, String displayNumber) {
}
