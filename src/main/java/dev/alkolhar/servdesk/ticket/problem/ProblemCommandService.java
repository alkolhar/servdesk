package dev.alkolhar.servdesk.ticket.problem;

import dev.alkolhar.servdesk.classification.PriorityDefinitionRepository;
import dev.alkolhar.servdesk.customfield.AttributeValidator;
import dev.alkolhar.servdesk.ticket.AbstractTicketSubtypeCommandService;
import dev.alkolhar.servdesk.ticket.SlaHooks;
import dev.alkolhar.servdesk.ticket.Ticket;
import dev.alkolhar.servdesk.ticket.TicketLifecycle;
import dev.alkolhar.servdesk.ticket.TicketRepository;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProblemCommandService extends AbstractTicketSubtypeCommandService<Problem> {

	private final ProblemRepository problemRepository;
	private final ProblemQueryService problemQueryService;

	public ProblemCommandService(ProblemRepository problemRepository, ProblemQueryService problemQueryService,
			TicketRepository ticketRepository, EntityManager entityManager, TicketLifecycle lifecycle,
			PriorityDefinitionRepository priorityDefinitionRepository, AttributeValidator attributeValidator,
			SlaHooks slaHooks) {
		super(ticketRepository, entityManager, lifecycle, priorityDefinitionRepository, attributeValidator, slaHooks);
		this.problemRepository = problemRepository;
		this.problemQueryService = problemQueryService;
	}

	@Transactional
	public Problem create(ProblemCreateRequest request) {
		Ticket savedTicket = ticketRepository.save(newTicket(request));
		Problem problem = new Problem();
		problem.setTicket(savedTicket);
		problem.setDisplayNumber(nextDisplayNumber("PRB-", "problem_number_seq"));
		return problemRepository.saveAndFlush(problem);
	}

	@Transactional
	public Problem update(Long id, ProblemUpdateRequest request) {
		Problem existing = problemQueryService.findById(id);
		applySharedUpdate(existing.getTicket(), request);
		applyRequestedStatus(existing.getTicket(), request.status());
		ticketRepository.save(existing.getTicket());
		return problemRepository.saveAndFlush(existing);
	}

	@Transactional
	public void delete(Long id) {
		Problem existing = problemQueryService.findById(id);
		deleteTicketAndSubtype(existing, existing.getTicket(), problemRepository);
	}
}
