package dev.alkolhar.servdesk.common.event;

/**
 * Marker for events published through Spring's
 * {@link org.springframework.context.ApplicationEventPublisher} after a state
 * change has been committed. Deliberately not sealed: feature modules define
 * their own event types (see {@code directory.event}, {@code ticket.event})
 * without this package needing to know about them upfront.
 */
public interface DomainEvent {
}
