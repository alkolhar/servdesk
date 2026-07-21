package dev.alkolhar.servdesk.ticket;

import dev.alkolhar.servdesk.common.exception.ForbiddenException;
import dev.alkolhar.servdesk.common.exception.NotFoundException;
import dev.alkolhar.servdesk.directory.Person;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;

/**
 * {@code authorId}/{@code authorIsAgent} are resolved by
 * {@code CommentController} from the authenticated caller — never
 * client-supplied — so a comment can't be authored as someone else and the
 * {@code internal} rejection below can't be bypassed by a client claiming to be
 * an Agent in the request body.
 */
@Service
public class CommentCommandService {

	private final TicketCommentRepository commentRepository;
	private final TicketRepository ticketRepository;
	private final EntityManager entityManager;
	private final SlaHooks slaHooks;

	public CommentCommandService(TicketCommentRepository commentRepository, TicketRepository ticketRepository,
			EntityManager entityManager, SlaHooks slaHooks) {
		this.commentRepository = commentRepository;
		this.ticketRepository = ticketRepository;
		this.entityManager = entityManager;
		this.slaHooks = slaHooks;
	}

	public TicketComment create(Long ticketId, CommentCreateRequest request, Long authorId, boolean authorIsAgent) {
		if (request.internal() && !authorIsAgent) {
			throw new ForbiddenException("Only an Agent can mark a comment internal");
		}
		Ticket ticket = ticketRepository.findById(ticketId)
				.orElseThrow(() -> new NotFoundException("Ticket " + ticketId + " not found"));
		if (!authorIsAgent && !authorId.equals(ticket.getRequester().getId())) {
			// 404, not 403: a Customer must not learn that a foreign ticket id exists
			throw new NotFoundException("Ticket " + ticketId + " not found");
		}
		// a non-internal Agent comment is the ticket's first response for SLA
		// purposes (issue #31) — internal notes aren't visible to the requester
		// and don't count
		if (authorIsAgent && !request.internal() && ticket.getFirstRespondedAt() == null) {
			slaHooks.recordAgentResponse(ticket);
			ticketRepository.save(ticket);
		}
		TicketComment comment = new TicketComment();
		comment.setTicket(ticket);
		comment.setAuthor(entityManager.getReference(Person.class, authorId));
		comment.setBody(request.body());
		comment.setInternal(request.internal());
		return commentRepository.save(comment);
	}
}
