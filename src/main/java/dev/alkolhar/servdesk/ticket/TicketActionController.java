package dev.alkolhar.servdesk.ticket;

import dev.alkolhar.servdesk.directory.PersonUserDetails;
import org.jspecify.annotations.Nullable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * {@code POST /api/tickets/{ticketId}/actions/{action}} (ADR-0008), nested
 * under the shared Ticket id like the comment stream, so one endpoint serves
 * every subtype. Clients don't build these URLs: they follow the ticket model's
 * {@code action:*} links, which exist only for what this caller may do now. 204
 * — the client reloads the ticket to see where the process took it.
 */
@RestController
@RequestMapping(value = "/api/tickets/{ticketId}/actions", version = "1")
public class TicketActionController {

	private final TicketActionService actionService;

	public TicketActionController(TicketActionService actionService) {
		this.actionService = actionService;
	}

	@PostMapping("/{action}")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void perform(@PathVariable Long ticketId, @PathVariable String action,
			@RequestBody(required = false) @Nullable ActionRequest request, Authentication authentication) {
		Long callerId = ((PersonUserDetails) authentication.getPrincipal()).getPerson().getId();
		actionService.perform(ticketId, action, request == null ? null : request.comment(), callerId);
	}

}
