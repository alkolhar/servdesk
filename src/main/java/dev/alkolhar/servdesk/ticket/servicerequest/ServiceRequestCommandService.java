package dev.alkolhar.servdesk.ticket.servicerequest;

import dev.alkolhar.servdesk.classification.PriorityDefinitionRepository;
import dev.alkolhar.servdesk.ticket.AbstractTicketSubtypeCommandService;
import dev.alkolhar.servdesk.ticket.Ticket;
import dev.alkolhar.servdesk.ticket.TicketRepository;
import jakarta.persistence.EntityManager;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class ServiceRequestCommandService extends AbstractTicketSubtypeCommandService<ServiceRequest> {

	private final ServiceRequestRepository serviceRequestRepository;
	private final ServiceRequestQueryService serviceRequestQueryService;

	public ServiceRequestCommandService(ServiceRequestRepository serviceRequestRepository,
			ServiceRequestQueryService serviceRequestQueryService, TicketRepository ticketRepository,
			EntityManager entityManager, ApplicationEventPublisher events,
			PriorityDefinitionRepository priorityDefinitionRepository) {
		super(ticketRepository, entityManager, events, priorityDefinitionRepository);
		this.serviceRequestRepository = serviceRequestRepository;
		this.serviceRequestQueryService = serviceRequestQueryService;
	}

	public ServiceRequest create(ServiceRequestCreateRequest request) {
		Ticket savedTicket = ticketRepository.save(newTicket(request));
		ServiceRequest serviceRequest = new ServiceRequest();
		serviceRequest.setTicket(savedTicket);
		serviceRequest.setDisplayNumber(nextDisplayNumber("REQ-", "service_request_number_seq"));
		return serviceRequestRepository.save(serviceRequest);
	}

	public ServiceRequest update(Long id, ServiceRequestUpdateRequest request) {
		ServiceRequest existing = serviceRequestQueryService.findById(id);
		applySharedUpdate(existing.getTicket(), request);
		ticketRepository.save(existing.getTicket());
		return serviceRequestRepository.save(existing);
	}

	public void delete(Long id) {
		ServiceRequest existing = serviceRequestQueryService.findById(id);
		deleteTicketAndSubtype(existing, existing.getTicket(), serviceRequestRepository);
	}
}
