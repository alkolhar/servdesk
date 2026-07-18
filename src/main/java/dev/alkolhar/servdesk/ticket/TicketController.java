package dev.alkolhar.servdesk.ticket;

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
@RequestMapping(value = "/api/tickets", version = "1")
public class TicketController {

	private final TicketCommandService commandService;
	private final TicketQueryService queryService;
	private final TicketModelAssembler assembler;

	public TicketController(TicketCommandService commandService, TicketQueryService queryService,
			TicketModelAssembler assembler) {
		this.commandService = commandService;
		this.queryService = queryService;
		this.assembler = assembler;
	}

	@GetMapping
	public PagedModel<TicketModel> findAll(@RequestParam(required = false) @Nullable TicketStatus status,
			@RequestParam(required = false) @Nullable TicketType type, @PageableDefault(size = 20) Pageable pageable,
			PagedResourcesAssembler<Ticket> pagedAssembler) {
		Page<Ticket> page = queryService.findAll(status, type, pageable);
		return pagedAssembler.toModel(page, assembler);
	}

	@GetMapping("/{id}")
	public TicketModel findById(@PathVariable Long id) {
		return assembler.toModel(queryService.findById(id));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public TicketModel create(@Valid @RequestBody TicketCreateRequest request) {
		return assembler.toModel(commandService.create(request));
	}

	@PutMapping("/{id}")
	public TicketModel update(@PathVariable Long id, @Valid @RequestBody TicketUpdateRequest request) {
		return assembler.toModel(commandService.update(id, request));
	}

	@DeleteMapping("/{id}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@PathVariable Long id) {
		commandService.delete(id);
	}
}
