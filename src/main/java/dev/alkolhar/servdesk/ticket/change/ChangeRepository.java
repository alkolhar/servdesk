package dev.alkolhar.servdesk.ticket.change;

import dev.alkolhar.servdesk.ticket.TicketStatus;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChangeRepository extends JpaRepository<Change, Long> {

	@Query("""
			select c from Change c
			where (:status is null or c.ticket.status = :status)
			and (:requesterId is null or c.ticket.requester.id = :requesterId)""")
	Page<Change> findVisible(@Param("status") @Nullable TicketStatus status,
			@Param("requesterId") @Nullable Long requesterId, Pageable pageable);
}
