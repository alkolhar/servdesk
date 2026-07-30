package dev.alkolhar.servdesk.sla;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import jakarta.validation.Valid;
import java.util.List;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/sla-policies", version = "1")
public class SlaPolicyController {

	private final SlaPolicyCommandService commandService;
	private final SlaPolicyQueryService queryService;
	private final SlaPolicyModelAssembler assembler;

	public SlaPolicyController(SlaPolicyCommandService commandService, SlaPolicyQueryService queryService,
			SlaPolicyModelAssembler assembler) {
		this.commandService = commandService;
		this.queryService = queryService;
		this.assembler = assembler;
	}

	@GetMapping
	public CollectionModel<SlaPolicyModel> findAll() {
		List<SlaPolicyModel> policies = queryService.findAll().stream().map(assembler::toModel).toList();
		return CollectionModel.of(policies, linkTo(methodOn(SlaPolicyController.class).findAll()).withSelfRel());
	}

	@GetMapping("/{id}")
	public SlaPolicyModel findById(@PathVariable Long id) {
		return assembler.toModel(queryService.findById(id));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public SlaPolicyModel create(@Valid @RequestBody SlaPolicyCreateRequest request) {
		return assembler.toModel(commandService.create(request));
	}

	@PutMapping("/{id}")
	public SlaPolicyModel update(@PathVariable Long id, @Valid @RequestBody SlaPolicyUpdateRequest request) {
		return assembler.toModel(commandService.update(id, request));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		commandService.delete(id);
	}
}
