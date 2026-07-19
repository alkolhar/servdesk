package dev.alkolhar.servdesk.classification;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
public class ImpactModelAssembler implements RepresentationModelAssembler<Impact, ImpactModel> {

	@Override
	public ImpactModel toModel(Impact impact) {
		ImpactModel model = new ImpactModel();
		model.setId(impact.getId());
		model.setName(impact.getName());
		model.setSortOrder(impact.getSortOrder());
		model.setCreatedAt(impact.getCreatedAt());
		model.setUpdatedAt(impact.getUpdatedAt());
		model.setCreatedBy(impact.getCreatedBy());
		model.setUpdatedBy(impact.getUpdatedBy());
		model.add(linkTo(methodOn(ImpactController.class).findById(impact.getId())).withSelfRel());
		return model;
	}
}
