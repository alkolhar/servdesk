package dev.alkolhar.servdesk.classification;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriorityDefinitionRepository extends JpaRepository<PriorityDefinition, Long> {

	/**
	 * Looked up by id pair rather than loading {@code Impact}/{@code Urgency} first
	 * — the caller (ticket subtype creation/update) only ever has the raw ids from
	 * the request, not managed references.
	 */
	Optional<PriorityDefinition> findByImpactIdAndUrgencyId(Long impactId, Long urgencyId);
}
