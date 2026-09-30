package dev.alkolhar.servdesk.ticket.servicerequest;

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
public class ServiceRequestCommandService extends AbstractTicketSubtypeCommandService<ServiceRequest> {

	private final ServiceRequestRepository serviceRequestRepository;
	private final ServiceRequestQueryService serviceRequestQueryService;

	public ServiceRequestCommandService(ServiceRequestRepository serviceRequestRepository,
			ServiceRequestQueryService serviceRequestQueryService, TicketRepository ticketRepository,
			EntityManager entityManager, TicketLifecycle lifecycle,
			PriorityDefinitionRepository priorityDefinitionRepository, AttributeValidator attributeValidator,
			SlaHooks slaHooks) {
		super(ticketRepository, entityManager, lifecycle, priorityDefinitionRepository, attributeValidator, slaHooks);
		this.serviceRequestRepository = serviceRequestRepository;
		this.serviceRequestQueryService = serviceRequestQueryService;
	}

	@Transactional
	public ServiceRequest create(ServiceRequestCreateRequest request) {
		Ticket savedTicket = ticketRepository.save(newTicket(request));
		ServiceRequest serviceRequest = new ServiceRequest();
		serviceRequest.setTicket(savedTicket);
		serviceRequest.setDisplayNumber(nextDisplayNumber("REQ-", "service_request_number_seq"));
		return serviceRequestRepository.saveAndFlush(serviceRequest);
	}

	@Transactional
	public ServiceRequest update(Long id, ServiceRequestUpdateRequest request) {
		ServiceRequest existing = serviceRequestQueryService.findById(id);
		applySharedUpdate(existing.getTicket(), request);
		applyRequestedStatus(existing.getTicket(), request.status());
		ticketRepository.save(existing.getTicket());
		return serviceRequestRepository.saveAndFlush(existing);
	}

	@Transactional
	public void delete(Long id) {
		ServiceRequest existing = serviceRequestQueryService.findById(id);
		deleteTicketAndSubtype(existing, existing.getTicket(), serviceRequestRepository);
	}
}
