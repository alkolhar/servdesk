package dev.alkolhar.servdesk.ticket.servicerequest;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import dev.alkolhar.servdesk.common.BaseEntity;
import dev.alkolhar.servdesk.directory.PersonController;
import dev.alkolhar.servdesk.ticket.Ticket;
import java.util.HashMap;
import org.jspecify.annotations.Nullable;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
public class ServiceRequestModelAssembler implements RepresentationModelAssembler<ServiceRequest, ServiceRequestModel> {

	@Override
	public ServiceRequestModel toModel(ServiceRequest serviceRequest) {
		Ticket ticket = serviceRequest.getTicket();
		ServiceRequestModel model = new ServiceRequestModel();
		model.setId(serviceRequest.getId());
		model.setDisplayNumber(serviceRequest.getDisplayNumber());
		model.setStatus(ticket.getStatus());
		model.setSubject(ticket.getSubject());
		model.setDescription(ticket.getDescription());
		model.setAttributes(new HashMap<>(ticket.getAttributes()));
		model.setCategoryId(idOf(ticket.getCategory()));
		model.setPriorityId(idOf(ticket.getPriority()));
		model.setRequesterId(idOf(ticket.getRequester()));
		model.setAssigneeId(idOf(ticket.getAssignee()));
		model.setTeamId(idOf(ticket.getTeam()));
		model.setResolvedAt(ticket.getResolvedAt());
		model.setClosedAt(ticket.getClosedAt());
		model.setRespondBy(ticket.getRespondBy());
		model.setResolveBy(ticket.getResolveBy());
		model.setFirstRespondedAt(ticket.getFirstRespondedAt());
		model.setResponseBreachedAt(ticket.getResponseBreachedAt());
		model.setResolutionBreachedAt(ticket.getResolutionBreachedAt());
		model.setCreatedAt(ticket.getCreatedAt());
		model.setUpdatedAt(ticket.getUpdatedAt());
		model.setCreatedBy(ticket.getCreatedBy());
		model.setUpdatedBy(ticket.getUpdatedBy());

		model.add(
				linkTo(methodOn(ServiceRequestController.class).findById(serviceRequest.getId(), null)).withSelfRel());
		model.add(
				linkTo(methodOn(PersonController.class).findById(ticket.getRequester().getId())).withRel("requester"));
		if (ticket.getAssignee() != null) {
			model.add(linkTo(methodOn(PersonController.class).findById(ticket.getAssignee().getId()))
					.withRel("assignee"));
		}
		return model;
	}

	private @Nullable Long idOf(@Nullable BaseEntity entity) {
		return entity == null ? null : entity.getId();
	}
}
