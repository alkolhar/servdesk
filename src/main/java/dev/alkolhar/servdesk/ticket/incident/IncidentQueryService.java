package dev.alkolhar.servdesk.ticket.incident;

import dev.alkolhar.servdesk.ticket.AbstractTicketSubtypeQueryService;
import dev.alkolhar.servdesk.ticket.Ticket;
import dev.alkolhar.servdesk.ticket.TicketStatus;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
public class IncidentQueryService extends AbstractTicketSubtypeQueryService<Incident> {

	private final IncidentRepository incidentRepository;

	public IncidentQueryService(IncidentRepository incidentRepository) {
		this.incidentRepository = incidentRepository;
	}

	public Page<Incident> findAll(@Nullable TicketStatus status, Long callerId, boolean callerIsAgent,
			Pageable pageable) {
		return incidentRepository.findVisible(status, requesterScope(callerId, callerIsAgent), pageable);
	}

	@Override
	protected JpaRepository<Incident, Long> repository() {
		return incidentRepository;
	}

	@Override
	protected Ticket ticketOf(Incident entity) {
		return entity.getTicket();
	}

	@Override
	protected String entityName() {
		return "Incident";
	}
}
