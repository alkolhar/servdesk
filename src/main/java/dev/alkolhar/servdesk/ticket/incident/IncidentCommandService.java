package dev.alkolhar.servdesk.ticket.incident;

import dev.alkolhar.servdesk.customfield.AttributeValidator;
import dev.alkolhar.servdesk.ticket.AbstractTicketSubtypeCommandService;
import dev.alkolhar.servdesk.ticket.Ticket;
import dev.alkolhar.servdesk.ticket.TicketRepository;
import dev.alkolhar.servdesk.ticket.problem.Problem;
import jakarta.persistence.EntityManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class IncidentCommandService extends AbstractTicketSubtypeCommandService<Incident> {

	private final IncidentRepository incidentRepository;
	private final IncidentQueryService incidentQueryService;

	public IncidentCommandService(IncidentRepository incidentRepository, IncidentQueryService incidentQueryService,
			TicketRepository ticketRepository, EntityManager entityManager, ApplicationEventPublisher events,
			AttributeValidator attributeValidator) {
		super(ticketRepository, entityManager, events, attributeValidator);
		this.incidentRepository = incidentRepository;
		this.incidentQueryService = incidentQueryService;
	}

	public Incident create(IncidentCreateRequest request) {
		Ticket savedTicket = ticketRepository.save(newTicket(request));
		Incident incident = new Incident();
		incident.setTicket(savedTicket);
		incident.setDisplayNumber(nextDisplayNumber("INC-", "incident_number_seq"));
		incident.setRelatedProblem(resolveReference(Problem.class, request.relatedProblemId()));
		return incidentRepository.save(incident);
	}

	public Incident update(Long id, IncidentUpdateRequest request) {
		Incident existing = incidentQueryService.findById(id);
		applySharedUpdate(existing.getTicket(), request);
		existing.setRelatedProblem(resolveReference(Problem.class, request.relatedProblemId()));
		ticketRepository.save(existing.getTicket());
		return incidentRepository.save(existing);
	}

	public void delete(Long id) {
		Incident existing = incidentQueryService.findById(id);
		deleteTicketAndSubtype(existing, existing.getTicket(), incidentRepository);
	}
}
