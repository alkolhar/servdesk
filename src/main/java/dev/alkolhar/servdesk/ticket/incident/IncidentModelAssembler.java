package dev.alkolhar.servdesk.ticket.incident;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import dev.alkolhar.servdesk.common.BaseEntity;
import dev.alkolhar.servdesk.directory.PersonController;
import dev.alkolhar.servdesk.ticket.Ticket;
import dev.alkolhar.servdesk.ticket.problem.Problem;
import dev.alkolhar.servdesk.ticket.problem.ProblemController;
import org.jspecify.annotations.Nullable;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
public class IncidentModelAssembler implements RepresentationModelAssembler<Incident, IncidentModel> {

	@Override
	public IncidentModel toModel(Incident incident) {
		Ticket ticket = incident.getTicket();
		Problem relatedProblem = incident.getRelatedProblem();
		IncidentModel model = new IncidentModel();
		model.setId(incident.getId());
		model.setDisplayNumber(incident.getDisplayNumber());
		model.setStatus(ticket.getStatus());
		model.setSubject(ticket.getSubject());
		model.setDescription(ticket.getDescription());
		model.setCategoryId(idOf(ticket.getCategory()));
		model.setPriorityId(idOf(ticket.getPriority()));
		model.setRequesterId(idOf(ticket.getRequester()));
		model.setAssigneeId(idOf(ticket.getAssignee()));
		model.setTeamId(idOf(ticket.getTeam()));
		model.setRelatedProblemId(relatedProblem == null ? null : relatedProblem.getId());
		model.setResolvedAt(ticket.getResolvedAt());
		model.setClosedAt(ticket.getClosedAt());
		model.setCreatedAt(ticket.getCreatedAt());
		model.setUpdatedAt(ticket.getUpdatedAt());
		model.setCreatedBy(ticket.getCreatedBy());
		model.setUpdatedBy(ticket.getUpdatedBy());

		model.add(linkTo(methodOn(IncidentController.class).findById(incident.getId(), null)).withSelfRel());
		model.add(
				linkTo(methodOn(PersonController.class).findById(ticket.getRequester().getId())).withRel("requester"));
		if (ticket.getAssignee() != null) {
			model.add(linkTo(methodOn(PersonController.class).findById(ticket.getAssignee().getId()))
					.withRel("assignee"));
		}
		if (relatedProblem != null) {
			model.add(linkTo(methodOn(ProblemController.class).findById(relatedProblem.getId(), null))
					.withRel("relatedProblem"));
		}
		return model;
	}

	private @Nullable Long idOf(@Nullable BaseEntity entity) {
		return entity == null ? null : entity.getId();
	}
}
