package dev.alkolhar.servdesk.sla;

import dev.alkolhar.servdesk.ticket.Ticket;
import dev.alkolhar.servdesk.ticket.TicketRepository;
import java.time.Instant;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * One scanner pass: stamp every newly-breached ticket's
 * {@code responseBreachedAt}/{@code resolutionBreachedAt} and publish one
 * {@link SlaBreachedEvent} each. The stamps are the idempotence mechanism — a
 * breach fires exactly once, across scanner runs and application restarts. Runs
 * {@code @Transactional} (the only service method that is): the stamps and the
 * pass are one atomic unit, and the {@code @TransactionalEventListener}
 * consumers of {@link SlaBreachedEvent} only fire on commit.
 */
@Service
public class SlaScanService {

	private final TicketRepository ticketRepository;
	private final ApplicationEventPublisher events;

	public SlaScanService(TicketRepository ticketRepository, ApplicationEventPublisher events) {
		this.ticketRepository = ticketRepository;
		this.events = events;
	}

	@Transactional
	public void scan(Instant now) {
		for (Ticket ticket : ticketRepository.findResponseBreaches(now)) {
			ticket.setResponseBreachedAt(now);
			ticketRepository.save(ticket);
			events.publishEvent(new SlaBreachedEvent(ticket.getId(), SlaBreachType.RESPONSE));
		}
		for (Ticket ticket : ticketRepository.findResolutionBreaches(now)) {
			ticket.setResolutionBreachedAt(now);
			ticketRepository.save(ticket);
			events.publishEvent(new SlaBreachedEvent(ticket.getId(), SlaBreachType.RESOLUTION));
		}
	}
}
