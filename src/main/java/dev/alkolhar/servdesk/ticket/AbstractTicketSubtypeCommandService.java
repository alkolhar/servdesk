package dev.alkolhar.servdesk.ticket;

import dev.alkolhar.servdesk.classification.Category;
import dev.alkolhar.servdesk.classification.Impact;
import dev.alkolhar.servdesk.classification.Priority;
import dev.alkolhar.servdesk.classification.PriorityDefinition;
import dev.alkolhar.servdesk.classification.PriorityDefinitionRepository;
import dev.alkolhar.servdesk.classification.Urgency;
import dev.alkolhar.servdesk.common.MapsIdBaseEntity;
import dev.alkolhar.servdesk.customfield.AttributeTarget;
import dev.alkolhar.servdesk.customfield.AttributeValidator;
import dev.alkolhar.servdesk.directory.Person;
import dev.alkolhar.servdesk.directory.Team;
import jakarta.persistence.EntityManager;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Shared CRUD for the fields every ticket subtype (Incident, Problem, Change,
 * Service Request) composes with via the shared {@link Ticket} record — see
 * ADR-0001. Handles resolving requester/assignee/team/category/impact/urgency
 * ids to managed references, deriving {@code priority} from the impact/urgency
 * pair via a {@link PriorityDefinition} matrix lookup. Status is not written
 * here: {@link TicketLifecycle} is its only writer (ADR-0004), called by the
 * subtype's lifecycle process — or, for a subtype not yet on one, from its
 * update request via {@link #applyRequestedStatus}. Each concrete subtype's own
 * command service extends this for the shared-field handling and adds only
 * what's genuinely subtype-specific: instantiating its own entity, assigning
 * its own prefixed display number from its own DB sequence, and any field of
 * its own (e.g. {@code Incident.relatedProblem}).
 * <p>
 * <b>Every subtype's {@code create}/{@code update}/{@code delete} is
 * {@code @Transactional}</b> (issue #127): each writes the shared
 * {@link Ticket} row and the subtype row, and the two are one unit. Committed
 * separately, a failed subtype insert (e.g. a {@code relatedProblemId} that
 * doesn't exist) left an orphan {@code Ticket} behind, and
 * {@code GET /api/tickets} then answered 500 for every page that included it.
 * The last write is {@code saveAndFlush}, so a constraint violation still
 * surfaces inside the method as {@code DataIntegrityViolationException} (409)
 * rather than at commit. Being inside a transaction is also what lets
 * {@code TicketStatusChangedEvent} reach its
 * {@code @TransactionalEventListener}s, and what a lifecycle process start
 * joins.
 */
public abstract class AbstractTicketSubtypeCommandService<T extends MapsIdBaseEntity> {

	protected final TicketRepository ticketRepository;
	protected final EntityManager entityManager;
	private final TicketLifecycle lifecycle;
	private final PriorityDefinitionRepository priorityDefinitionRepository;
	private final AttributeValidator attributeValidator;
	private final SlaHooks slaHooks;

	protected AbstractTicketSubtypeCommandService(TicketRepository ticketRepository, EntityManager entityManager,
			TicketLifecycle lifecycle, PriorityDefinitionRepository priorityDefinitionRepository,
			AttributeValidator attributeValidator, SlaHooks slaHooks) {
		this.ticketRepository = ticketRepository;
		this.entityManager = entityManager;
		this.lifecycle = lifecycle;
		this.priorityDefinitionRepository = priorityDefinitionRepository;
		this.attributeValidator = attributeValidator;
		this.slaHooks = slaHooks;
	}

	protected Ticket newTicket(TicketCreateFields fields) {
		Ticket ticket = new Ticket();
		copySharedFields(ticket, fields);
		slaHooks.applyOnWrite(ticket, null, null);
		return ticket;
	}

	/**
	 * The descriptive fields only — never the status. {@code previousPriorityId} is
	 * read <i>before</i> {@link #copySharedFields} re-derives the priority from the
	 * incoming impact/urgency pair, so {@link SlaHooks#applyOnWrite} sees the real
	 * before/after and re-stamps the deadlines exactly when the matrix actually
	 * moved the ticket to a different priority. A client can no longer change the
	 * priority directly (issue #22), so a derived change is the only kind there is.
	 * The status hasn't moved here, so the SLA pause is untouched.
	 */
	protected void applySharedUpdate(Ticket ticket, TicketCreateFields fields) {
		Long previousPriorityId = ticket.getPriority() == null ? null : ticket.getPriority().getId();
		copySharedFields(ticket, fields);
		slaHooks.applyOnWrite(ticket, ticket.getStatus(), previousPriorityId);
	}

	/**
	 * A status from the update request, for the subtypes whose lifecycle isn't on a
	 * process yet (Problem, Change, Service Request — each until its own slice).
	 * Called after {@link #applySharedUpdate}, which keeps the SLA order it always
	 * had: deadlines re-derived for a new priority first, then shifted for a pause
	 * that ends.
	 */
	protected void applyRequestedStatus(Ticket ticket, TicketStatus status) {
		lifecycle.applyStatus(ticket, status);
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
		long next = ((Number) entityManager.createNativeQuery("SELECT nextval('" + sequenceName + "')")
				.getSingleResult()).longValue();
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
		// null/omitted means empty — full replacement, consistent with PUT semantics
		Map<String, Object> attributes = fields.attributes() == null
				? new HashMap<>()
				: new HashMap<>(fields.attributes());
		attributeValidator.validate(AttributeTarget.TICKET, attributes);
		ticket.setAttributes(attributes);
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
}
