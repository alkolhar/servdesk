package dev.alkolhar.servdesk.ticket.incident;

import dev.alkolhar.servdesk.classification.PriorityDefinitionRepository;
import dev.alkolhar.servdesk.customfield.AttributeValidator;
import dev.alkolhar.servdesk.ticket.AbstractTicketSubtypeCommandService;
import dev.alkolhar.servdesk.ticket.SlaHooks;
import dev.alkolhar.servdesk.ticket.Ticket;
import dev.alkolhar.servdesk.ticket.TicketLifecycle;
import dev.alkolhar.servdesk.ticket.TicketProcesses;
import dev.alkolhar.servdesk.ticket.TicketRepository;
import dev.alkolhar.servdesk.ticket.problem.Problem;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IncidentCommandService extends AbstractTicketSubtypeCommandService<Incident> {

	/**
	 * The Incident lifecycle, {@code processes/ticket-incident.bpmn20.xml}
	 * (ADR-0007).
	 */
	public static final String PROCESS_KEY = "ticket-incident";

	private final IncidentRepository incidentRepository;
	private final IncidentQueryService incidentQueryService;
	private final TicketProcesses processes;

	public IncidentCommandService(IncidentRepository incidentRepository, IncidentQueryService incidentQueryService,
			TicketRepository ticketRepository, EntityManager entityManager, TicketLifecycle lifecycle,
			PriorityDefinitionRepository priorityDefinitionRepository, AttributeValidator attributeValidator,
			SlaHooks slaHooks, TicketProcesses processes) {
		super(ticketRepository, entityManager, lifecycle, priorityDefinitionRepository, attributeValidator, slaHooks);
		this.incidentRepository = incidentRepository;
		this.incidentQueryService = incidentQueryService;
		this.processes = processes;
	}

	@Transactional
	public Incident create(IncidentCreateRequest request) {
		Ticket savedTicket = ticketRepository.save(newTicket(request));
		Incident incident = new Incident();
		incident.setTicket(savedTicket);
		incident.setDisplayNumber(nextDisplayNumber("INC-", "incident_number_seq"));
		incident.setRelatedProblem(resolveReference(Problem.class, request.relatedProblemId()));
		Incident saved = incidentRepository.saveAndFlush(incident);
		// same transaction: no Incident without its process, and the process sets the
		// status from its first stage on (ADR-0004)
		processes.start(PROCESS_KEY, savedTicket);
		return saved;
	}

	@Transactional
	public Incident update(Long id, IncidentUpdateRequest request) {
		Incident existing = incidentQueryService.findById(id);
		applySharedUpdate(existing.getTicket(), request);
		existing.setRelatedProblem(resolveReference(Problem.class, request.relatedProblemId()));
		ticketRepository.save(existing.getTicket());
		return incidentRepository.saveAndFlush(existing);
	}

	@Transactional
	public void delete(Long id) {
		Incident existing = incidentQueryService.findById(id);
		processes.end(PROCESS_KEY, existing.getTicket(), "Incident deleted");
		deleteTicketAndSubtype(existing, existing.getTicket(), incidentRepository);
	}
}
