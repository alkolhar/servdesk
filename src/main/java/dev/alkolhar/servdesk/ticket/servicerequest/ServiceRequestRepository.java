package dev.alkolhar.servdesk.ticket.servicerequest;

import dev.alkolhar.servdesk.ticket.TicketStatus;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {

	@Query("""
			select s from ServiceRequest s
			where (:status is null or s.ticket.status = :status)
			and (:requesterId is null or s.ticket.requester.id = :requesterId)""")
	Page<ServiceRequest> findVisible(@Param("status") @Nullable TicketStatus status,
			@Param("requesterId") @Nullable Long requesterId, Pageable pageable);
}
