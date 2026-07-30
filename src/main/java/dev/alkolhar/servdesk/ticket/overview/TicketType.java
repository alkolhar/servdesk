package dev.alkolhar.servdesk.ticket.overview;

/**
 * Which subtype a shared ticket row belongs to. Purely a read-model
 * discriminator: the domain itself has no type field (ADR-0001 — a subtype
 * row's existence says what kind of ticket it is); this enum only labels
 * overview responses.
 */
public enum TicketType {
	INCIDENT, PROBLEM, CHANGE, SERVICE_REQUEST
}
