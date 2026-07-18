package dev.alkolhar.servdesk.directory;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import org.springframework.hateoas.server.RepresentationModelAssembler;
import org.springframework.stereotype.Component;

@Component
public class PersonModelAssembler implements RepresentationModelAssembler<Person, PersonModel> {

	@Override
	public PersonModel toModel(Person person) {
		PersonModel model = new PersonModel();
		model.setId(person.getId());
		model.setRole(person.getRole());
		model.setName(person.getName());
		model.setEmail(person.getEmail());
		model.setPhone(person.getPhone());
		model.setUsername(person.getUsername());
		model.setEnabled(person.isEnabled());
		model.setTeamId(person.getTeam() == null ? null : person.getTeam().getId());
		model.setCreatedAt(person.getCreatedAt());
		model.setUpdatedAt(person.getUpdatedAt());
		model.setCreatedBy(person.getCreatedBy());
		model.setUpdatedBy(person.getUpdatedBy());
		model.add(linkTo(methodOn(PersonController.class).findById(person.getId())).withSelfRel());
		return model;
	}
}
