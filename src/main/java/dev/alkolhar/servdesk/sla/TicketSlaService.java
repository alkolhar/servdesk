package dev.alkolhar.servdesk.sla;

import dev.alkolhar.servdesk.ticket.SlaHooks;
import dev.alkolhar.servdesk.ticket.Ticket;
import dev.alkolhar.servdesk.ticket.TicketStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

/**
 * The deadline arithmetic behind {@link SlaHooks} — v1 runs a 24/7 clock;
 * business-hours calendars replace the plain {@code plus(minutes)} here without
 * touching any caller.
 * <p>
 * Deadlines are (re)computed only when the priority actually changed, anchored
 * at {@code createdAt} ({@code now} on the very first write, before
 * {@code @CreationTimestamp} has run). Known v1 imprecision, documented on
 * issue #31: a priority change drops pause credit accumulated in earlier
 * PENDING phases, since pauses are stored as shifts inside the deadlines
 * themselves.
 */
@Service
public class TicketSlaService implements SlaHooks {

	private final SlaPolicyRepository policyRepository;

	public TicketSlaService(SlaPolicyRepository policyRepository) {
		this.policyRepository = policyRepository;
	}

	@Override
	public void applyOnWrite(Ticket ticket, @Nullable TicketStatus previousStatus, @Nullable Long previousPriorityId) {
		Instant now = Instant.now();
		Long priorityId = ticket.getPriority() == null ? null : ticket.getPriority().getId();
		boolean isCreate = previousStatus == null;
		if (isCreate || !Objects.equals(priorityId, previousPriorityId)) {
			deriveDeadlines(ticket, priorityId, now);
		}
		applyPendingTransition(ticket, previousStatus, now);
	}

	@Override
	public void recordAgentResponse(Ticket ticket) {
		if (ticket.getFirstRespondedAt() == null) {
			ticket.setFirstRespondedAt(Instant.now());
		}
	}

	private void deriveDeadlines(Ticket ticket, @Nullable Long priorityId, Instant now) {
		SlaPolicy policy = priorityId == null ? null : policyRepository.findByPriorityId(priorityId).orElse(null);
		Instant anchor = ticket.getCreatedAt() == null ? now : ticket.getCreatedAt();
		ticket.setRespondBy(deadline(anchor, policy == null ? null : policy.getResponseMinutes()));
		ticket.setResolveBy(deadline(anchor, policy == null ? null : policy.getResolutionMinutes()));
		// a fresh target may or may not already be breached — let the scanner decide
		// again
		ticket.setResponseBreachedAt(null);
		ticket.setResolutionBreachedAt(null);
	}

	private @Nullable Instant deadline(Instant anchor, @Nullable Integer minutes) {
		return minutes == null ? null : anchor.plus(Duration.ofMinutes(minutes));
	}

	private void applyPendingTransition(Ticket ticket, @Nullable TicketStatus previousStatus, Instant now) {
		boolean wasPending = previousStatus == TicketStatus.PENDING;
		boolean isPending = ticket.getStatus() == TicketStatus.PENDING;
		if (!wasPending && isPending) {
			ticket.setPendingSince(now);
		} else if (wasPending && !isPending && ticket.getPendingSince() != null) {
			Duration paused = Duration.between(ticket.getPendingSince(), now);
			if (ticket.getRespondBy() != null) {
				ticket.setRespondBy(ticket.getRespondBy().plus(paused));
			}
			if (ticket.getResolveBy() != null) {
				ticket.setResolveBy(ticket.getResolveBy().plus(paused));
			}
			ticket.setPendingSince(null);
		}
	}
}
