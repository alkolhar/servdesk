package dev.alkolhar.servdesk.directory.event;

import dev.alkolhar.servdesk.common.event.DomainEvent;
import dev.alkolhar.servdesk.directory.PersonRole;

public record PersonCreatedEvent(Long personId, PersonRole role) implements DomainEvent {
}
