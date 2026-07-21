package dev.alkolhar.servdesk.ticket.incident;

import dev.alkolhar.servdesk.ticket.TicketStatus;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface IncidentRepository extends JpaRepository<Incident, Long> {

	@Query("""
			select i from Incident i
			where (:status is null or i.ticket.status = :status)
			and (:requesterId is null or i.ticket.requester.id = :requesterId)""")
	Page<Incident> findVisible(@Param("status") @Nullable TicketStatus status,
			@Param("requesterId") @Nullable Long requesterId, Pageable pageable);
}
