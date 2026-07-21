package dev.alkolhar.servdesk.ticket.overview;

import dev.alkolhar.servdesk.directory.Person;
import dev.alkolhar.servdesk.directory.PersonRole;
import dev.alkolhar.servdesk.directory.PersonUserDetails;
import dev.alkolhar.servdesk.ticket.TicketStatus;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedResourcesAssembler;
import org.springframework.hateoas.PagedModel;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Read-only: creating, updating, and deleting stay on each subtype's own
 * endpoint (ADR-0001 with its issue-#30 amendment). Nested next to
 * {@code CommentController}'s {@code /api/tickets/{ticketId}/comments} — both
 * address the shared ticket id.
 */
@RestController
@RequestMapping(value = "/api/tickets", version = "1")
public class TicketController {

	private final TicketQueryService queryService;
	private final TicketModelAssembler assembler;

	public TicketController(TicketQueryService queryService, TicketModelAssembler assembler) {
		this.queryService = queryService;
		this.assembler = assembler;
	}

	@GetMapping
	public PagedModel<TicketModel> findAll(@RequestParam(required = false) @Nullable TicketStatus status,
			@RequestParam(required = false) @Nullable Long requesterId,
			@RequestParam(required = false) @Nullable Long assigneeId,
			@RequestParam(required = false) @Nullable Long teamId,
			@RequestParam(required = false) @Nullable Long categoryId,
			@RequestParam(required = false) @Nullable Long priorityId,
			@RequestParam(required = false) @Nullable String attrKey,
			@RequestParam(required = false) @Nullable String attrValue, @PageableDefault(size = 20) Pageable pageable,
			PagedResourcesAssembler<TicketOverview> pagedAssembler, Authentication authentication) {
		Person caller = callerOf(authentication);
		Page<TicketOverview> page = queryService.findAll(status, requesterId, assigneeId, teamId, categoryId,
				priorityId, attrKey, attrValue, caller.getId(), caller.getRole() == PersonRole.AGENT, pageable);
		return pagedAssembler.toModel(page, assembler);
	}

	@GetMapping("/{id}")
	public TicketModel findById(@PathVariable Long id, Authentication authentication) {
		Person caller = callerOf(authentication);
		return assembler
				.toModel(queryService.findByIdVisibleTo(id, caller.getId(), caller.getRole() == PersonRole.AGENT));
	}

	private Person callerOf(Authentication authentication) {
		return ((PersonUserDetails) authentication.getPrincipal()).getPerson();
	}
}
