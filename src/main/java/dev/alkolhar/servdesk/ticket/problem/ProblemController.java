package dev.alkolhar.servdesk.ticket.problem;

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
@RequestMapping(value = "/api/problems", version = "1")
public class ProblemController {

	private final ProblemCommandService commandService;
	private final ProblemQueryService queryService;
	private final ProblemModelAssembler assembler;

	public ProblemController(ProblemCommandService commandService, ProblemQueryService queryService,
			ProblemModelAssembler assembler) {
		this.commandService = commandService;
		this.queryService = queryService;
		this.assembler = assembler;
	}

	@GetMapping
	public PagedModel<ProblemModel> findAll(@RequestParam(required = false) @Nullable TicketStatus status,
			@PageableDefault(size = 20) Pageable pageable, PagedResourcesAssembler<Problem> pagedAssembler,
			Authentication authentication) {
		Person caller = callerOf(authentication);
		Page<Problem> page = queryService.findAll(status, caller.getId(), caller.getRole() == PersonRole.AGENT,
				pageable);
		return pagedAssembler.toModel(page, assembler);
	}

	@GetMapping("/{id}")
	public ProblemModel findById(@PathVariable Long id, Authentication authentication) {
		Person caller = callerOf(authentication);
		return assembler
				.toModel(queryService.findByIdVisibleTo(id, caller.getId(), caller.getRole() == PersonRole.AGENT));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public ProblemModel create(@Valid @RequestBody ProblemCreateRequest request) {
		return assembler.toModel(commandService.create(request));
	}

	@PutMapping("/{id}")
	public ProblemModel update(@PathVariable Long id, @Valid @RequestBody ProblemUpdateRequest request) {
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
