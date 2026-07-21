package dev.alkolhar.servdesk.ticket.problem;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import dev.alkolhar.servdesk.common.BaseEntity;
import dev.alkolhar.servdesk.directory.PersonController;
import dev.alkolhar.servdesk.ticket.Ticket;
import org.jspecify.annotations.Nullable;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
public class ProblemModelAssembler implements RepresentationModelAssembler<Problem, ProblemModel> {

	@Override
	public ProblemModel toModel(Problem problem) {
		Ticket ticket = problem.getTicket();
		ProblemModel model = new ProblemModel();
		model.setId(problem.getId());
		model.setDisplayNumber(problem.getDisplayNumber());
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

		model.add(linkTo(methodOn(ProblemController.class).findById(problem.getId(), null)).withSelfRel());
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
