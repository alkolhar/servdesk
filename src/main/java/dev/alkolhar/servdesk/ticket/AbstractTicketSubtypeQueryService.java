package dev.alkolhar.servdesk.ticket;

import dev.alkolhar.servdesk.common.MapsIdBaseEntity;
import dev.alkolhar.servdesk.common.exception.NotFoundException;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Shared {@code findById} lookup for every ticket subtype, wrapping a missing
 * row in {@link NotFoundException} the same way across all four. Filtered
 * listing (by {@link TicketStatus}) is left to each concrete query service: it
 * delegates to a query method on its own repository (e.g.
 * {@code IncidentRepository.findByOptionalStatus}), since the JPQL has to name
 * the concrete subtype entity.
 */
public abstract class AbstractTicketSubtypeQueryService<T extends MapsIdBaseEntity> {

	public T findById(Long id) {
		return repository().findById(id)
				.orElseThrow(() -> new NotFoundException(entityName() + " " + id + " not found"));
	}

	protected abstract JpaRepository<T, Long> repository();

	protected abstract String entityName();
}
