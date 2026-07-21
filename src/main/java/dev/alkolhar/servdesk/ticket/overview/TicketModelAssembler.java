package dev.alkolhar.servdesk.ticket.overview;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import dev.alkolhar.servdesk.common.BaseEntity;
import dev.alkolhar.servdesk.ticket.CommentController;
import dev.alkolhar.servdesk.ticket.Ticket;
import dev.alkolhar.servdesk.ticket.change.ChangeController;
import dev.alkolhar.servdesk.ticket.incident.IncidentController;
import dev.alkolhar.servdesk.ticket.problem.ProblemController;
import dev.alkolhar.servdesk.ticket.servicerequest.ServiceRequestController;
import java.util.HashMap;
import org.jspecify.annotations.Nullable;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
public class TicketModelAssembler implements RepresentationModelAssembler<TicketOverview, TicketModel> {

	@Override
	public TicketModel toModel(TicketOverview overview) {
		Ticket ticket = overview.ticket();
		TicketModel model = new TicketModel();
		model.setId(ticket.getId());
		model.setType(overview.type());
		model.setDisplayNumber(overview.displayNumber());
		model.setStatus(ticket.getStatus());
		model.setSubject(ticket.getSubject());
		model.setDescription(ticket.getDescription());
		model.setAttributes(new HashMap<>(ticket.getAttributes()));
		model.setCategoryId(idOf(ticket.getCategory()));
		model.setPriorityId(idOf(ticket.getPriority()));
		model.setRequesterId(ticket.getRequester().getId());
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

		Long id = ticket.getId();
		model.add(linkTo(methodOn(TicketController.class).findById(id, null)).withSelfRel());
		model.add(subtypeLink(overview.type(), id));
		model.add(linkTo(methodOn(CommentController.class).findAll(id, null)).withRel("comments"));
		return model;
	}

	/**
	 * The link to the ticket's full subtype resource, its rel named after the type
	 * — the overview deliberately carries no subtype-specific field, so this is how
	 * a client reaches e.g. {@code Incident.relatedProblemId}.
	 */
	private Link subtypeLink(TicketType type, Long id) {
		return switch (type) {
			case INCIDENT -> linkTo(methodOn(IncidentController.class).findById(id, null)).withRel("incident");
			case PROBLEM -> linkTo(methodOn(ProblemController.class).findById(id, null)).withRel("problem");
			case CHANGE -> linkTo(methodOn(ChangeController.class).findById(id, null)).withRel("change");
			case SERVICE_REQUEST ->
				linkTo(methodOn(ServiceRequestController.class).findById(id, null)).withRel("serviceRequest");
		};
	}

	private @Nullable Long idOf(@Nullable BaseEntity entity) {
		return entity == null ? null : entity.getId();
	}
}
