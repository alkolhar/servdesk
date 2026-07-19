package dev.alkolhar.servdesk.classification;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
public class CategoryModelAssembler implements RepresentationModelAssembler<Category, CategoryModel> {

	@Override
	public CategoryModel toModel(Category category) {
		CategoryModel model = new CategoryModel();
		model.setId(category.getId());
		model.setName(category.getName());
		Category parent = category.getParent();
		model.setParentId(parent == null ? null : parent.getId());
		model.setCreatedAt(category.getCreatedAt());
		model.setUpdatedAt(category.getUpdatedAt());
		model.setCreatedBy(category.getCreatedBy());
		model.setUpdatedBy(category.getUpdatedBy());
		model.add(linkTo(methodOn(CategoryController.class).findById(category.getId())).withSelfRel());
		if (parent != null) {
			model.add(linkTo(methodOn(CategoryController.class).findById(parent.getId())).withRel("parent"));
		}
		return model;
	}
}
