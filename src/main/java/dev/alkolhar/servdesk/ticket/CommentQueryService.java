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
	 * {@code includeInternal} is resolved by {@code CommentController} from the
	 * authenticated caller's role — a Customer only ever sees non-internal comments
	 * on their own ticket; an Agent sees both in one combined, chronological
	 * stream.
	 */
	public List<TicketComment> findByTicket(Long ticketId, boolean includeInternal) {
		ticketRepository.findById(ticketId)
				.orElseThrow(() -> new NotFoundException("Ticket " + ticketId + " not found"));
		List<TicketComment> comments = commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId);
		return includeInternal ? comments : comments.stream().filter(comment -> !comment.isInternal()).toList();
	}
}
