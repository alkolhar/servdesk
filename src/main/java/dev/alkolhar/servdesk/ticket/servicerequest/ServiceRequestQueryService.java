package dev.alkolhar.servdesk.ticket.servicerequest;

import dev.alkolhar.servdesk.ticket.AbstractTicketSubtypeQueryService;
import dev.alkolhar.servdesk.ticket.TicketStatus;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

@Service
public class ServiceRequestQueryService extends AbstractTicketSubtypeQueryService<ServiceRequest> {

	private final ServiceRequestRepository serviceRequestRepository;

	public ServiceRequestQueryService(ServiceRequestRepository serviceRequestRepository) {
		this.serviceRequestRepository = serviceRequestRepository;
	}

	public Page<ServiceRequest> findAll(@Nullable TicketStatus status, Pageable pageable) {
		return serviceRequestRepository.findByOptionalStatus(status, pageable);
	}

	@Override
	protected JpaRepository<ServiceRequest, Long> repository() {
		return serviceRequestRepository;
	}

	@Override
	protected String entityName() {
		return "Service Request";
	}
}
