package dev.alkolhar.servdesk.ticket;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

	@Query("select t from Ticket t where (:status is null or t.status = :status) "
			+ "and (:type is null or t.type = :type)")
	Page<Ticket> findByOptionalFilters(@Param("status") @Nullable TicketStatus status,
			@Param("type") @Nullable TicketType type, Pageable pageable);
}
