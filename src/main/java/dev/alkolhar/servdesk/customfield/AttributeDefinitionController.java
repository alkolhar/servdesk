package dev.alkolhar.servdesk.customfield;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import jakarta.validation.Valid;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/attribute-definitions", version = "1")
public class AttributeDefinitionController {

	private final AttributeDefinitionCommandService commandService;
	private final AttributeDefinitionQueryService queryService;
	private final AttributeDefinitionModelAssembler assembler;

	public AttributeDefinitionController(AttributeDefinitionCommandService commandService,
			AttributeDefinitionQueryService queryService, AttributeDefinitionModelAssembler assembler) {
		this.commandService = commandService;
		this.queryService = queryService;
		this.assembler = assembler;
	}

	@GetMapping
	public CollectionModel<AttributeDefinitionModel> findAll(
			@RequestParam(required = false) @Nullable AttributeTarget target) {
		List<AttributeDefinitionModel> definitions = queryService.findAll(target).stream().map(assembler::toModel)
				.toList();
		return CollectionModel.of(definitions,
				linkTo(methodOn(AttributeDefinitionController.class).findAll(null)).withSelfRel());
	}

	@GetMapping("/{id}")
	public AttributeDefinitionModel findById(@PathVariable Long id) {
		return assembler.toModel(queryService.findById(id));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public AttributeDefinitionModel create(@Valid @RequestBody AttributeDefinitionCreateRequest request) {
		return assembler.toModel(commandService.create(request));
	}

	@PutMapping("/{id}")
	public AttributeDefinitionModel update(@PathVariable Long id,
			@Valid @RequestBody AttributeDefinitionUpdateRequest request) {
		return assembler.toModel(commandService.update(id, request));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		commandService.delete(id);
	}
}
