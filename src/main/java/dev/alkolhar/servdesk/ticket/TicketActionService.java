package dev.alkolhar.servdesk.ticket;

import dev.alkolhar.servdesk.common.exception.ConflictException;
import dev.alkolhar.servdesk.common.exception.NotFoundException;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Task-backed ticket actions (ADR-0008): an action completes the ticket's
 * current task with that outcome, and the process moves the ticket on. Which
 * actions exist is the process's business ({@link TicketTasks}); what this adds
 * is the API's rules around them.
 */
@Service
public class TicketActionService {

	/**
	 * The resolution note, and the reason a ticket is closed without being fixed or
	 * a Change is turned down (ADR-0008).
	 */
	static final Set<String> COMMENT_REQUIRED = Set.of("resolve", "cancel", "reject");

	private final TicketRepository ticketRepository;
	private final TicketTasks ticketTasks;
	private final CommentCommandService commentCommandService;

	public TicketActionService(TicketRepository ticketRepository, TicketTasks ticketTasks,
			CommentCommandService commentCommandService) {
		this.ticketRepository = ticketRepository;
		this.ticketTasks = ticketTasks;
		this.commentCommandService = commentCommandService;
	}

	/**
	 * Performs {@code action} on the ticket as {@code callerId} (an Agent's Person
	 * id — only Agents may act, see {@code SecurityConfig}). The comment, when
	 * given, becomes an ordinary non-internal comment by the caller, in the same
	 * transaction as the step it explains.
	 *
	 * @throws NotFoundException
	 *             no such ticket
	 * @throws ConflictException
	 *             the action isn't available on this ticket now — a different
	 *             stage, or a ticket whose process has ended
	 * @throws IllegalArgumentException
	 *             the action needs a comment and none was given (400)
	 */
	@Transactional
	public void perform(Long ticketId, String action, @Nullable String comment, Long callerId) {
		if (!ticketRepository.existsById(ticketId)) {
			throw new NotFoundException("Ticket " + ticketId + " not found");
		}
		TicketTasks.OpenTask task = ticketTasks.openTasks(ticketId).stream()
				.filter(open -> open.actions().contains(action)).findFirst().orElseThrow(() -> new ConflictException(
						"Action '" + action + "' is not available on ticket " + ticketId + " in its current stage"));
		boolean hasComment = comment != null && !comment.isBlank();
		if (COMMENT_REQUIRED.contains(action) && !hasComment) {
			throw new IllegalArgumentException("Action '" + action + "' requires a comment");
		}
		if (hasComment) {
			commentCommandService.create(ticketId, new CommentCreateRequest(comment, false), callerId, true);
		}
		ticketTasks.complete(task, action, callerId);
	}

}
