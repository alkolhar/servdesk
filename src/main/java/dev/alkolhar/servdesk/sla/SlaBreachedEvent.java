package dev.alkolhar.servdesk.sla;

import dev.alkolhar.servdesk.common.event.DomainEvent;

/**
 * Published (once per ticket and breach kind — see the {@code *BreachedAt}
 * idempotence markers) when {@code SlaScanService} finds a deadline in the
 * past. Notification/watcher listeners attach with #25; today
 * {@code DomainEventLogger} proves the wiring.
 */
public record SlaBreachedEvent(Long ticketId, SlaBreachType type) implements DomainEvent {
}
