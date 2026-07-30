package dev.alkolhar.servdesk.ticket;

import dev.alkolhar.servdesk.common.exception.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class CommentQueryService {

	private final TicketCommentRepository commentRepository;
	private final TicketRepository ticketRepository;

	public CommentQueryService(TicketCommentRepository commentRepository, TicketRepository ticketRepository) {
		this.commentRepository = commentRepository;
		this.ticketRepository = ticketRepository;
	}

	/**
	 * {@code callerId}/{@code callerIsAgent} are resolved by
	 * {@code CommentController} from the authenticated caller. A Customer can only
	 * read the comment stream of a ticket they requested — a foreign ticket answers
	 * 404, exactly like a missing one, so the ticket id's existence isn't leaked —
	 * and within their own stream only sees non-internal comments; an Agent sees
	 * every ticket's full stream.
	 */
	public List<TicketComment> findByTicket(Long ticketId, Long callerId, boolean callerIsAgent) {
		Ticket ticket = ticketRepository.findById(ticketId)
				.orElseThrow(() -> new NotFoundException("Ticket " + ticketId + " not found"));
		if (!callerIsAgent && !callerId.equals(ticket.getRequester().getId())) {
			throw new NotFoundException("Ticket " + ticketId + " not found");
		}
		List<TicketComment> comments = commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);
		return callerIsAgent ? comments : comments.stream().filter(comment -> !comment.isInternal()).toList();
	}
}
