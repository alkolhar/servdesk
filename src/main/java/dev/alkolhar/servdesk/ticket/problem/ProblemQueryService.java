package dev.alkolhar.servdesk.ticket.problem;

import dev.alkolhar.servdesk.ticket.AbstractTicketSubtypeQueryService;
import dev.alkolhar.servdesk.ticket.Ticket;
import dev.alkolhar.servdesk.ticket.TicketStatus;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
public class ProblemQueryService extends AbstractTicketSubtypeQueryService<Problem> {

	private final ProblemRepository problemRepository;

	public ProblemQueryService(ProblemRepository problemRepository) {
		this.problemRepository = problemRepository;
	}

	public Page<Problem> findAll(@Nullable TicketStatus status, Long callerId, boolean callerIsAgent,
			Pageable pageable) {
		return problemRepository.findVisible(status, requesterScope(callerId, callerIsAgent), pageable);
	}

	@Override
	protected JpaRepository<Problem, Long> repository() {
		return problemRepository;
	}

	@Override
	protected Ticket ticketOf(Problem entity) {
		return entity.getTicket();
	}

	@Override
	protected String entityName() {
		return "Problem";
	}
}
