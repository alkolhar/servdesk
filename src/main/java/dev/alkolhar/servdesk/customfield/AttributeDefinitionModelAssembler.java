package dev.alkolhar.servdesk.customfield;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import java.util.List;
import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
public class AttributeDefinitionModelAssembler
		implements
			RepresentationModelAssembler<AttributeDefinition, AttributeDefinitionModel> {

	@Override
	public AttributeDefinitionModel toModel(AttributeDefinition definition) {
		AttributeDefinitionModel model = new AttributeDefinitionModel();
		model.setId(definition.getId());
		model.setTarget(definition.getTarget());
		model.setKey(definition.getKey());
		model.setLabel(definition.getLabel());
		model.setType(definition.getType());
		model.setRequired(definition.isRequired());
		List<String> enumValues = definition.getEnumValues();
		model.setEnumValues(enumValues == null ? null : List.copyOf(enumValues));
		model.setCreatedAt(definition.getCreatedAt());
		model.setUpdatedAt(definition.getUpdatedAt());
		model.setCreatedBy(definition.getCreatedBy());
		model.setUpdatedBy(definition.getUpdatedBy());
		model.add(linkTo(methodOn(AttributeDefinitionController.class).findById(definition.getId())).withSelfRel());
		return model;
	}
}
