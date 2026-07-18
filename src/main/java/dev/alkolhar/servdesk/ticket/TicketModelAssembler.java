package dev.alkolhar.servdesk.ticket;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import dev.alkolhar.servdesk.common.BaseEntity;
import dev.alkolhar.servdesk.directory.PersonController;
import org.jspecify.annotations.Nullable;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
public class TicketModelAssembler implements RepresentationModelAssembler<Ticket, TicketModel> {

	@Override
	public TicketModel toModel(Ticket ticket) {
		TicketModel model = new TicketModel();
		model.setId(ticket.getId());
		model.setTicketNumber(ticket.getTicketNumber());
		model.setType(ticket.getType());
		model.setStatus(ticket.getStatus());
		model.setSubject(ticket.getSubject());
		model.setDescription(ticket.getDescription());
		model.setCategoryId(idOf(ticket.getCategory()));
		model.setPriorityId(idOf(ticket.getPriority()));
		model.setRequesterId(idOf(ticket.getRequester()));
		model.setAssigneeId(idOf(ticket.getAssignee()));
		model.setTeamId(idOf(ticket.getTeam()));
		model.setResolvedAt(ticket.getResolvedAt());
		model.setClosedAt(ticket.getClosedAt());
		model.setCreatedAt(ticket.getCreatedAt());
		model.setUpdatedAt(ticket.getUpdatedAt());
		model.setCreatedBy(ticket.getCreatedBy());
		model.setUpdatedBy(ticket.getUpdatedBy());

		model.add(linkTo(methodOn(TicketController.class).findById(ticket.getId())).withSelfRel());
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
