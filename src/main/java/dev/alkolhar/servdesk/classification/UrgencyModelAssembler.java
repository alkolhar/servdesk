package dev.alkolhar.servdesk.classification;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
public class UrgencyModelAssembler implements RepresentationModelAssembler<Urgency, UrgencyModel> {

	@Override
	public UrgencyModel toModel(Urgency urgency) {
		UrgencyModel model = new UrgencyModel();
		model.setId(urgency.getId());
		model.setName(urgency.getName());
		model.setSortOrder(urgency.getSortOrder());
		model.setCreatedAt(urgency.getCreatedAt());
		model.setUpdatedAt(urgency.getUpdatedAt());
		model.setCreatedBy(urgency.getCreatedBy());
		model.setUpdatedBy(urgency.getUpdatedBy());
		model.add(linkTo(methodOn(UrgencyController.class).findById(urgency.getId())).withSelfRel());
		return model;
	}
}
