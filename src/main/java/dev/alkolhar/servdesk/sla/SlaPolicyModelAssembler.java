package dev.alkolhar.servdesk.sla;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
public class SlaPolicyModelAssembler implements RepresentationModelAssembler<SlaPolicy, SlaPolicyModel> {

	@Override
	public SlaPolicyModel toModel(SlaPolicy policy) {
		SlaPolicyModel model = new SlaPolicyModel();
		model.setId(policy.getId());
		model.setPriorityId(policy.getPriority().getId());
		model.setResponseMinutes(policy.getResponseMinutes());
		model.setResolutionMinutes(policy.getResolutionMinutes());
		model.setCreatedAt(policy.getCreatedAt());
		model.setUpdatedAt(policy.getUpdatedAt());
		model.setCreatedBy(policy.getCreatedBy());
		model.setUpdatedBy(policy.getUpdatedBy());
		model.add(linkTo(methodOn(SlaPolicyController.class).findById(policy.getId())).withSelfRel());
		return model;
	}
}
