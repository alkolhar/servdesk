package dev.alkolhar.servdesk.ticket;

import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

	/**
	 * Cross-subtype listing over the shared ticket table (issue #30) with the same
	 * optional-filter idiom as each subtype repository's {@code findVisible}.
	 * {@code requesterScope} is the row-level ownership filter (null for an Agent,
	 * the caller's own id for a Customer) and is applied on top of the
	 * client-chosen {@code requesterId} filter — for a Customer both must hold, so
	 * filtering by someone else's requester id simply yields an empty page rather
	 * than leaking foreign tickets.
	 */
	@Query("""
			select t from Ticket t
			where (:status is null or t.status = :status)
			and (:requesterId is null or t.requester.id = :requesterId)
			and (:assigneeId is null or t.assignee.id = :assigneeId)
			and (:teamId is null or t.team.id = :teamId)
			and (:categoryId is null or t.category.id = :categoryId)
			and (:priorityId is null or t.priority.id = :priorityId)
			and (:requesterScope is null or t.requester.id = :requesterScope)""")
	Page<Ticket> findVisible(@Param("status") @Nullable TicketStatus status,
			@Param("requesterId") @Nullable Long requesterId, @Param("assigneeId") @Nullable Long assigneeId,
			@Param("teamId") @Nullable Long teamId, @Param("categoryId") @Nullable Long categoryId,
			@Param("priorityId") @Nullable Long priorityId, @Param("requesterScope") @Nullable Long requesterScope,
			Pageable pageable);
}
