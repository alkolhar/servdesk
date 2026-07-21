package dev.alkolhar.servdesk.ticket.problem;

import dev.alkolhar.servdesk.customfield.AttributeValidator;
import dev.alkolhar.servdesk.ticket.AbstractTicketSubtypeCommandService;
import dev.alkolhar.servdesk.ticket.Ticket;
import dev.alkolhar.servdesk.ticket.TicketRepository;
import jakarta.persistence.EntityManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class ProblemCommandService extends AbstractTicketSubtypeCommandService<Problem> {

	private final ProblemRepository problemRepository;
	private final ProblemQueryService problemQueryService;

	public ProblemCommandService(ProblemRepository problemRepository, ProblemQueryService problemQueryService,
			TicketRepository ticketRepository, EntityManager entityManager, ApplicationEventPublisher events,
			AttributeValidator attributeValidator) {
		super(ticketRepository, entityManager, events, attributeValidator);
		this.problemRepository = problemRepository;
		this.problemQueryService = problemQueryService;
	}

	public Problem create(ProblemCreateRequest request) {
		Ticket savedTicket = ticketRepository.save(newTicket(request));
		Problem problem = new Problem();
		problem.setTicket(savedTicket);
		problem.setDisplayNumber(nextDisplayNumber("PRB-", "problem_number_seq"));
		return problemRepository.save(problem);
	}

	public Problem update(Long id, ProblemUpdateRequest request) {
		Problem existing = problemQueryService.findById(id);
		applySharedUpdate(existing.getTicket(), request);
		ticketRepository.save(existing.getTicket());
		return problemRepository.save(existing);
	}

	public void delete(Long id) {
		Problem existing = problemQueryService.findById(id);
		deleteTicketAndSubtype(existing, existing.getTicket(), problemRepository);
	}
}
