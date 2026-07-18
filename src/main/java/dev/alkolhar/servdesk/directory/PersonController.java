package dev.alkolhar.servdesk.directory;

import jakarta.validation.Valid;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedModel;
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
@RequestMapping(value = "/api/persons", version = "1")
public class PersonController {

	private final PersonCommandService commandService;
	private final PersonQueryService queryService;
	private final PersonModelAssembler assembler;

	public PersonController(PersonCommandService commandService, PersonQueryService queryService,
			PersonModelAssembler assembler) {
		this.commandService = commandService;
		this.queryService = queryService;
		this.assembler = assembler;
	}

	@GetMapping
	public PagedModel<PersonModel> findAll(@RequestParam(required = false) @Nullable PersonRole role,
			@PageableDefault(size = 20) Pageable pageable, PagedResourcesAssembler<Person> pagedAssembler) {
		Page<Person> page = queryService.findAll(role, pageable);
		return pagedAssembler.toModel(page, assembler);
	}

	@GetMapping("/{id}")
	public PersonModel findById(@PathVariable Long id) {
		return assembler.toModel(queryService.findById(id));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public PersonModel create(@Valid @RequestBody PersonCreateRequest request) {
		return assembler.toModel(commandService.create(request));
	}

	@PutMapping("/{id}")
	public PersonModel update(@PathVariable Long id, @Valid @RequestBody PersonUpdateRequest request) {
		return assembler.toModel(commandService.update(id, request));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		commandService.delete(id);
	}
}
