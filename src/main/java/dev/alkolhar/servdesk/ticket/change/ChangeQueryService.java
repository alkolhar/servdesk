package dev.alkolhar.servdesk.ticket.change;

import dev.alkolhar.servdesk.ticket.AbstractTicketSubtypeQueryService;
import dev.alkolhar.servdesk.ticket.Ticket;
import dev.alkolhar.servdesk.ticket.TicketStatus;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
public class ChangeQueryService extends AbstractTicketSubtypeQueryService<Change> {

	private final ChangeRepository changeRepository;

	public ChangeQueryService(ChangeRepository changeRepository) {
		this.changeRepository = changeRepository;
	}

	public Page<Change> findAll(@Nullable TicketStatus status, Long callerId, boolean callerIsAgent,
			Pageable pageable) {
		return changeRepository.findVisible(status, requesterScope(callerId, callerIsAgent), pageable);
	}

	@Override
	protected JpaRepository<Change, Long> repository() {
		return changeRepository;
	}

	@Override
	protected Ticket ticketOf(Change entity) {
		return entity.getTicket();
	}

	@Override
	protected String entityName() {
		return "Change";
	}
}
