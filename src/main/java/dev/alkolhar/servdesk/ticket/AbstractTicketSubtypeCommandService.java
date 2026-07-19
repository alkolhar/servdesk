package dev.alkolhar.servdesk.ticket;

import dev.alkolhar.servdesk.classification.Category;
import dev.alkolhar.servdesk.classification.Impact;
import dev.alkolhar.servdesk.classification.Priority;
import dev.alkolhar.servdesk.classification.PriorityDefinition;
import dev.alkolhar.servdesk.classification.PriorityDefinitionRepository;
import dev.alkolhar.servdesk.classification.Urgency;
import dev.alkolhar.servdesk.common.MapsIdBaseEntity;
import dev.alkolhar.servdesk.directory.Person;
import dev.alkolhar.servdesk.directory.Team;
import dev.alkolhar.servdesk.ticket.event.TicketStatusChangedEvent;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Shared CRUD for the fields every ticket subtype (Incident, Problem, Change,
 * Service Request) composes with via the shared {@link Ticket} record — see
 * ADR-0001. Handles resolving requester/assignee/team/category/impact/urgency
 * ids to managed references, deriving {@code priority} from the impact/urgency
 * pair via a {@link PriorityDefinition} matrix lookup, and deriving
 * {@code resolvedAt}/{@code closedAt} from a status transition (publishing
 * {@link TicketStatusChangedEvent} only when the status actually changes,
 * clearing the corresponding timestamp on reopen). Each concrete subtype's own
 * command service extends this for the shared-field handling and adds only
 * what's genuinely subtype-specific: instantiating its own entity, assigning
 * its own prefixed display number from its own DB sequence, and any field of
 * its own (e.g. {@code Incident.relatedProblem}).
 */
public abstract class AbstractTicketSubtypeCommandService<T extends MapsIdBaseEntity> {

	protected final TicketRepository ticketRepository;
	protected final EntityManager entityManager;
	private final ApplicationEventPublisher events;
	private final PriorityDefinitionRepository priorityDefinitionRepository;

	protected AbstractTicketSubtypeCommandService(TicketRepository ticketRepository, EntityManager entityManager,
			ApplicationEventPublisher events, PriorityDefinitionRepository priorityDefinitionRepository) {
		this.ticketRepository = ticketRepository;
		this.entityManager = entityManager;
		this.events = events;
		this.priorityDefinitionRepository = priorityDefinitionRepository;
	}

	protected Ticket newTicket(TicketCreateFields fields) {
		Ticket ticket = new Ticket();
		copySharedFields(ticket, fields);
		return ticket;
	}

	protected void applySharedUpdate(Ticket ticket, TicketUpdateFields fields) {
		TicketStatus previousStatus = ticket.getStatus();
		copySharedFields(ticket, fields);
		ticket.setStatus(fields.status());
		deriveResolvedAndClosedAt(ticket, previousStatus, fields.status());
	}

	protected void deleteTicketAndSubtype(T subtype, Ticket ticket, JpaRepository<T, Long> repository) {
		repository.delete(subtype);
		ticketRepository.delete(ticket);
	}

	/**
	 * Drawn from the subtype's own DB sequence rather than the generated id, since
	 * the id is only known after insert but the display number is {@code NOT NULL}
	 * and must be set beforehand. Zero-padded by hand rather than via
	 * {@code String.format("%06d", ...)}: {@code Formatter} runs every integer
	 * conversion through the JVM's default-locale {@code DecimalFormatSymbols}
	 * (even with an explicit {@code Locale.ROOT}), which can substitute non-ASCII
	 * digit characters for some locales — wrong for a display number that must
	 * always be plain ASCII digits.
	 */
	protected String nextDisplayNumber(String prefix, String sequenceName) {
		long next = ((Number) entityManager.createNativeQuery("SELECT NEXTVAL(" + sequenceName + ")").getSingleResult())
				.longValue();
		String digits = Long.toString(next);
		return prefix + "0".repeat(Math.max(0, 6 - digits.length())) + digits;
	}

	/**
	 * Requests carry related entities (category/impact/urgency/requester/
	 * assignee/team, and any subtype-specific reference like
	 * {@code relatedProblemId}) as plain ids; resolve each to a managed proxy
	 * rather than loading the full row.
	 */
	protected <E> @Nullable E resolveReference(Class<E> type, @Nullable Long id) {
		return id == null ? null : entityManager.getReference(type, id);
	}

	private void copySharedFields(Ticket ticket, TicketCreateFields fields) {
		ticket.setSubject(fields.subject());
		ticket.setDescription(fields.description());
		ticket.setCategory(resolveReference(Category.class, fields.categoryId()));
		ticket.setImpact(resolveReference(Impact.class, fields.impactId()));
		ticket.setUrgency(resolveReference(Urgency.class, fields.urgencyId()));
		ticket.setPriority(derivePriority(fields.impactId(), fields.urgencyId()));
		ticket.setRequester(entityManager.getReference(Person.class, fields.requesterId()));
		ticket.setAssignee(resolveReference(Person.class, fields.assigneeId()));
		ticket.setTeam(resolveReference(Team.class, fields.teamId()));
	}

	/**
	 * Looked up by id pair, not by loading the {@code Impact}/{@code Urgency}
	 * references first — {@code fields.impactId()}/{@code fields.urgencyId()} are
	 * already the raw ids. Deliberately permissive: a null input or an unmapped
	 * pair (no matching {@link PriorityDefinition}) both resolve to {@code null}
	 * rather than rejecting the write — a gap in the matrix is an admin
	 * data-quality concern, not a reason to block ticket creation/update.
	 */
	private @Nullable Priority derivePriority(@Nullable Long impactId, @Nullable Long urgencyId) {
		if (impactId == null || urgencyId == null) {
			return null;
		}
		return priorityDefinitionRepository.findByImpactIdAndUrgencyId(impactId, urgencyId)
				.map(PriorityDefinition::getPriority).orElse(null);
	}

	/**
	 * Relies on {@link TicketStatus}'s declared enum order (OPEN, IN_PROGRESS,
	 * PENDING, RESOLVED, CLOSED) matching the ticket lifecycle's actual order, so
	 * {@code ordinal()} can distinguish "reopened to an earlier status" (clear the
	 * timestamp) from "progressed forward" (e.g. RESOLVED to CLOSED, where
	 * {@code resolvedAt} must survive rather than being wiped just because the
	 * status moved on).
	 */
	private void deriveResolvedAndClosedAt(Ticket ticket, TicketStatus previousStatus, TicketStatus newStatus) {
		if (previousStatus == newStatus) {
			return;
		}
		Instant now = Instant.now();
		if (newStatus == TicketStatus.RESOLVED) {
			ticket.setResolvedAt(now);
		} else if (newStatus.ordinal() < TicketStatus.RESOLVED.ordinal()) {
			ticket.setResolvedAt(null);
		}
		ticket.setClosedAt(newStatus == TicketStatus.CLOSED ? now : null);
		events.publishEvent(new TicketStatusChangedEvent(ticket.getId(), previousStatus, newStatus));
	}
}
