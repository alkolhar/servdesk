package dev.alkolhar.servdesk.ticket;

import dev.alkolhar.servdesk.common.MapsIdBaseEntity;
import dev.alkolhar.servdesk.common.exception.NotFoundException;
import org.jspecify.annotations.Nullable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Shared {@code findById} lookup for every ticket subtype, wrapping a missing
 * row in {@link NotFoundException} the same way across all four. Filtered
 * listing (by {@link TicketStatus}) is left to each concrete query service: it
 * delegates to a query method on its own repository (e.g.
 * {@code IncidentRepository.findVisible}), since the JPQL has to name the
 * concrete subtype entity.
 * <p>
 * Row-level ownership lives here in the service layer, not in
 * {@code SecurityConfig}: whether a Customer may see a ticket depends on the
 * ticket's own {@code requester} — a data-dependent decision no static URL+role
 * rule can express (same reasoning as the internal-comment rule in
 * {@code CommentCommandService}). A Customer sees only tickets they requested;
 * an Agent sees all. A foreign ticket answers 404, not 403 — a Customer must
 * not learn that someone else's ticket id exists.
 */
public abstract class AbstractTicketSubtypeQueryService<T extends MapsIdBaseEntity> {

	public T findById(Long id) {
		return repository().findById(id)
				.orElseThrow(() -> new NotFoundException(entityName() + " " + id + " not found"));
	}

	public T findByIdVisibleTo(Long id, Long callerId, boolean callerIsAgent) {
		T entity = findById(id);
		if (!callerIsAgent && !callerId.equals(ticketOf(entity).getRequester().getId())) {
			throw new NotFoundException(entityName() + " " + id + " not found");
		}
		return entity;
	}

	/**
	 * The requester filter a listing must apply for this caller: {@code null} (no
	 * filter) for an Agent, the caller's own id for a Customer.
	 */
	protected static @Nullable Long requesterScope(Long callerId, boolean callerIsAgent) {
		return callerIsAgent ? null : callerId;
	}

	protected abstract JpaRepository<T, Long> repository();

	protected abstract Ticket ticketOf(T entity);

	protected abstract String entityName();
}
