package dev.alkolhar.servdesk.ticket;

import org.jspecify.annotations.Nullable;

/**
 * The ticket package's view of the SLA engine, implemented by
 * {@code sla.TicketSlaService}. An interface owned <i>here</i> rather than a
 * direct dependency on the {@code sla} package: {@code sla} already depends on
 * {@code ticket} (it derives deadlines from {@link Ticket} rows), so the
 * command layer calling into {@code sla} directly would create the package
 * cycle {@code ArchitectureTest} forbids — same dependency-inversion shape as
 * the rest of the layering rules.
 */
public interface SlaHooks {

	/**
	 * Derive/adjust the ticket's SLA state on any create or update: (re)compute
	 * {@code respondBy}/{@code resolveBy} when the priority actually changed
	 * (anchored at {@code createdAt}), record {@code pendingSince} on entering
	 * {@code PENDING}, and shift both deadlines by the paused duration on leaving
	 * it. Called after the shared fields and status have been applied, before the
	 * ticket is saved.
	 */
	void applyOnWrite(Ticket ticket, @Nullable TicketStatus previousStatus, @Nullable Long previousPriorityId);

	/**
	 * Record the first non-internal Agent comment as the ticket's first response
	 * ({@code firstRespondedAt}) — a no-op on every later call. Callers persist the
	 * ticket afterwards.
	 */
	void recordAgentResponse(Ticket ticket);
}
