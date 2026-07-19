package dev.alkolhar.servdesk.classification;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
public class PriorityDefinitionModelAssembler
		implements
			RepresentationModelAssembler<PriorityDefinition, PriorityDefinitionModel> {

	@Override
	public PriorityDefinitionModel toModel(PriorityDefinition definition) {
		PriorityDefinitionModel model = new PriorityDefinitionModel();
		model.setId(definition.getId());
		model.setImpactId(definition.getImpact().getId());
		model.setUrgencyId(definition.getUrgency().getId());
		model.setPriorityId(definition.getPriority().getId());
		model.setCreatedAt(definition.getCreatedAt());
		model.setUpdatedAt(definition.getUpdatedAt());
		model.setCreatedBy(definition.getCreatedBy());
		model.setUpdatedBy(definition.getUpdatedBy());

		model.add(linkTo(methodOn(PriorityDefinitionController.class).findById(definition.getId())).withSelfRel());
		model.add(linkTo(methodOn(ImpactController.class).findById(definition.getImpact().getId())).withRel("impact"));
		model.add(
				linkTo(methodOn(UrgencyController.class).findById(definition.getUrgency().getId())).withRel("urgency"));
		model.add(linkTo(methodOn(PriorityController.class).findById(definition.getPriority().getId()))
				.withRel("priority"));
		return model;
	}
}
