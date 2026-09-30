package dev.alkolhar.servdesk.ticket;

import dev.alkolhar.servdesk.ticket.event.TicketStatusChangedEvent;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

/**
 * The only code that writes {@code Ticket.status} (ADR-0004). A lifecycle
 * process calls it on entering each stage — "the process decides <i>when</i>,
 * Java decides <i>what it means</i>" — and so, until their own slices, does the
 * status field on the Problem/Change/Service Request update requests.
 * <p>
 * Every side effect of a status change lives here, comparing explicit previous
 * and new statuses (no enum ordinals): {@code resolvedAt}/{@code closedAt}, the
 * SLA pause and resume through {@link SlaHooks} (so {@code ticket} still never
 * imports {@code sla}), and {@link TicketStatusChangedEvent}. Entering the
 * status a ticket already has changes nothing and publishes nothing.
 * <p>
 * Runs in its caller's transaction: a process step's when the BPMN calls it,
 * the update request's otherwise.
 */
@Service
public class TicketLifecycle {

	/** Entering one of these means the ticket isn't resolved (any more). */
	private static final Set<TicketStatus> UNRESOLVED = EnumSet.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS,
			TicketStatus.PENDING);

	private final TicketRepository ticketRepository;
	private final SlaHooks slaHooks;
	private final ApplicationEventPublisher events;

	public TicketLifecycle(TicketRepository ticketRepository, SlaHooks slaHooks, ApplicationEventPublisher events) {
		this.ticketRepository = ticketRepository;
		this.slaHooks = slaHooks;
		this.events = events;
	}

	/**
	 * The BPMN's entry point, from a stage's start execution listener:
	 * {@code ${ticketLifecycle.enterStatus(execution.processInstanceBusinessKey, 'IN_PROGRESS')}}.
	 * The business key is the shared {@link Ticket} id.
	 */
	public void enterStatus(Long ticketId, TicketStatus newStatus) {
		Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(() -> new IllegalStateException(
				"Ticket " + ticketId + " has a running lifecycle process but no live row"));
		applyStatus(ticket, newStatus);
		ticketRepository.save(ticket);
	}

	/**
	 * For callers that already hold the managed ticket; they persist it. Named
	 * apart from {@link #enterStatus(Long, TicketStatus)} on purpose: the BPMN's
	 * expression language picks a method by name and argument count, so two
	 * two-argument {@code enterStatus}es would be ambiguous there.
	 */
	public void applyStatus(Ticket ticket, TicketStatus newStatus) {
		TicketStatus previousStatus = ticket.getStatus();
		if (previousStatus == newStatus) {
			return;
		}
		ticket.setStatus(newStatus);
		Instant now = Instant.now();
		if (newStatus == TicketStatus.RESOLVED) {
			ticket.setResolvedAt(now);
		} else if (UNRESOLVED.contains(newStatus)) {
			// reopened: whatever resolved it no longer holds
			ticket.setResolvedAt(null);
		}
		// CLOSED keeps resolvedAt: a ticket closed after being resolved stays resolved,
		// and one closed without it (cancelled) never was
		ticket.setClosedAt(newStatus == TicketStatus.CLOSED ? now : null);
		slaHooks.applyOnWrite(ticket, previousStatus, priorityIdOf(ticket));
		events.publishEvent(new TicketStatusChangedEvent(ticket.getId(), previousStatus, newStatus));
	}

	/**
	 * The priority isn't changing here, so SLA deadlines are only paused or
	 * resumed.
	 */
	private static @Nullable Long priorityIdOf(Ticket ticket) {
		return ticket.getPriority() == null ? null : ticket.getPriority().getId();
	}

}
