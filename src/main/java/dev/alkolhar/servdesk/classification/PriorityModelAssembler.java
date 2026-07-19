package dev.alkolhar.servdesk.classification;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
public class PriorityModelAssembler implements RepresentationModelAssembler<Priority, PriorityModel> {

	@Override
	public PriorityModel toModel(Priority priority) {
		PriorityModel model = new PriorityModel();
		model.setId(priority.getId());
		model.setName(priority.getName());
		model.setSortOrder(priority.getSortOrder());
		model.setCreatedAt(priority.getCreatedAt());
		model.setUpdatedAt(priority.getUpdatedAt());
		model.setCreatedBy(priority.getCreatedBy());
		model.setUpdatedBy(priority.getUpdatedBy());
		model.add(linkTo(methodOn(PriorityController.class).findById(priority.getId())).withSelfRel());
		return model;
	}
}
