package dev.alkolhar.servdesk.ticket.change;

import dev.alkolhar.servdesk.customfield.AttributeValidator;
import dev.alkolhar.servdesk.ticket.AbstractTicketSubtypeCommandService;
import dev.alkolhar.servdesk.ticket.Ticket;
import dev.alkolhar.servdesk.ticket.TicketRepository;
import jakarta.persistence.EntityManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class ChangeCommandService extends AbstractTicketSubtypeCommandService<Change> {

	private final ChangeRepository changeRepository;
	private final ChangeQueryService changeQueryService;

	public ChangeCommandService(ChangeRepository changeRepository, ChangeQueryService changeQueryService,
			TicketRepository ticketRepository, EntityManager entityManager, ApplicationEventPublisher events,
			AttributeValidator attributeValidator) {
		super(ticketRepository, entityManager, events, attributeValidator);
		this.changeRepository = changeRepository;
		this.changeQueryService = changeQueryService;
	}

	public Change create(ChangeCreateRequest request) {
		Ticket savedTicket = ticketRepository.save(newTicket(request));
		Change change = new Change();
		change.setTicket(savedTicket);
		change.setDisplayNumber(nextDisplayNumber("RFC-", "change_number_seq"));
		return changeRepository.save(change);
	}

	public Change update(Long id, ChangeUpdateRequest request) {
		Change existing = changeQueryService.findById(id);
		applySharedUpdate(existing.getTicket(), request);
		ticketRepository.save(existing.getTicket());
		return changeRepository.save(existing);
	}

	public void delete(Long id) {
		Change existing = changeQueryService.findById(id);
		deleteTicketAndSubtype(existing, existing.getTicket(), changeRepository);
	}
}
