package dev.alkolhar.servdesk.ticket;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.methodOn;

import dev.alkolhar.servdesk.directory.Person;
import dev.alkolhar.servdesk.directory.PersonRole;
import dev.alkolhar.servdesk.directory.PersonUserDetails;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.hateoas.CollectionModel;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Nested under the shared {@link Ticket} id (not per-subtype), since
 * {@link TicketComment} references {@code Ticket} directly and serves all four
 * subtypes uniformly — see ADR-0001.
 */
@RestController
@RequestMapping(value = "/api/tickets/{ticketId}/comments", version = "1")
public class CommentController {

	private final CommentCommandService commandService;
	private final CommentQueryService queryService;
	private final CommentModelAssembler assembler;

	public CommentController(CommentCommandService commandService, CommentQueryService queryService,
			CommentModelAssembler assembler) {
		this.commandService = commandService;
		this.queryService = queryService;
		this.assembler = assembler;
	}

	@GetMapping
	public CollectionModel<CommentModel> findAll(@PathVariable Long ticketId, Authentication authentication) {
		Person caller = callerOf(authentication);
		List<CommentModel> comments = queryService.findByTicket(ticketId, caller.getRole() == PersonRole.AGENT).stream()
				.map(assembler::toModel).toList();
		return CollectionModel.of(comments,
				linkTo(methodOn(CommentController.class).findAll(ticketId, null)).withSelfRel());
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public CommentModel create(@PathVariable Long ticketId, @Valid @RequestBody CommentCreateRequest request,
			Authentication authentication) {
		Person caller = callerOf(authentication);
		TicketComment created = commandService.create(ticketId, request, caller.getId(),
				caller.getRole() == PersonRole.AGENT);
		return assembler.toModel(created);
	}

	private Person callerOf(Authentication authentication) {
		return ((PersonUserDetails) authentication.getPrincipal()).getPerson();
	}
}
