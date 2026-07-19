package dev.alkolhar.servdesk.ticket.problem;

import dev.alkolhar.servdesk.ticket.TicketStatus;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProblemRepository extends JpaRepository<Problem, Long> {

	@Query("select p from Problem p where (:status is null or p.ticket.status = :status)")
	Page<Problem> findByOptionalStatus(@Param("status") @Nullable TicketStatus status, Pageable pageable);
}
