package dev.alkolhar.servdesk.common.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Placeholder consumer proving the publish/subscribe wiring end-to-end. Future
 * modules (SLA, Notifications) replace or complement this with real listeners;
 * they should follow the same {@link TransactionalEventListener} pattern so
 * they never act on an event whose transaction rolled back.
 */
@Component
public class DomainEventLogger {

	private static final Logger log = LoggerFactory.getLogger(DomainEventLogger.class);

	@TransactionalEventListener
	public void onDomainEvent(DomainEvent event) {
		log.debug("Domain event published: {}", event);
	}
}
