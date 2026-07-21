package dev.alkolhar.servdesk.ticket.change;

import dev.alkolhar.servdesk.directory.Person;
import dev.alkolhar.servdesk.directory.PersonRole;
import dev.alkolhar.servdesk.directory.PersonUserDetails;
import dev.alkolhar.servdesk.ticket.TicketStatus;
import jakarta.validation.Valid;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
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
@RequestMapping(value = "/api/changes", version = "1")
public class ChangeController {

	private final ChangeCommandService commandService;
	private final ChangeQueryService queryService;
	private final ChangeModelAssembler assembler;

	public ChangeController(ChangeCommandService commandService, ChangeQueryService queryService,
			ChangeModelAssembler assembler) {
		this.commandService = commandService;
		this.queryService = queryService;
		this.assembler = assembler;
	}

	@GetMapping
	public PagedModel<ChangeModel> findAll(@RequestParam(required = false) @Nullable TicketStatus status,
			@PageableDefault(size = 20) Pageable pageable, PagedResourcesAssembler<Change> pagedAssembler,
			Authentication authentication) {
		Person caller = callerOf(authentication);
		Page<Change> page = queryService.findAll(status, caller.getId(), caller.getRole() == PersonRole.AGENT,
				pageable);
		return pagedAssembler.toModel(page, assembler);
	}

	@GetMapping("/{id}")
	public ChangeModel findById(@PathVariable Long id, Authentication authentication) {
		Person caller = callerOf(authentication);
		return assembler
				.toModel(queryService.findByIdVisibleTo(id, caller.getId(), caller.getRole() == PersonRole.AGENT));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ChangeModel create(@Valid @RequestBody ChangeCreateRequest request) {
		return assembler.toModel(commandService.create(request));
	}

	@PutMapping("/{id}")
	public ChangeModel update(@PathVariable Long id, @Valid @RequestBody ChangeUpdateRequest request) {
		return assembler.toModel(commandService.update(id, request));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		commandService.delete(id);
	}

	private Person callerOf(Authentication authentication) {
		return ((PersonUserDetails) authentication.getPrincipal()).getPerson();
	}
}
