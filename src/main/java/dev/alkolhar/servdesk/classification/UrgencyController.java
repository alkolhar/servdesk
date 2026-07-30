package dev.alkolhar.servdesk.classification;

import jakarta.validation.Valid;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/urgencies", version = "1")
public class UrgencyController {

	private final UrgencyCommandService commandService;
	private final UrgencyQueryService queryService;
	private final UrgencyModelAssembler assembler;

	public UrgencyController(UrgencyCommandService commandService, UrgencyQueryService queryService,
			UrgencyModelAssembler assembler) {
		this.commandService = commandService;
		this.queryService = queryService;
		this.assembler = assembler;
	}

	@GetMapping
	public PagedModel<UrgencyModel> findAll(@PageableDefault(size = 20) Pageable pageable,
			PagedResourcesAssembler<Urgency> pagedAssembler) {
		Page<Urgency> page = queryService.findAll(pageable);
		return pagedAssembler.toModel(page, assembler);
	}

	@GetMapping("/{id}")
	public UrgencyModel findById(@PathVariable Long id) {
		return assembler.toModel(queryService.findById(id));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public UrgencyModel create(@Valid @RequestBody UrgencyCreateRequest request) {
		return assembler.toModel(commandService.create(request));
	}

	@PutMapping("/{id}")
	public UrgencyModel update(@PathVariable Long id, @Valid @RequestBody UrgencyUpdateRequest request) {
		return assembler.toModel(commandService.update(id, request));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		commandService.delete(id);
	}
}
