package dev.alkolhar.servdesk.sla;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SlaPolicyRepository extends JpaRepository<SlaPolicy, Long> {

	Optional<SlaPolicy> findByPriorityId(Long priorityId);

	boolean existsByPriorityId(Long priorityId);
}
