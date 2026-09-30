package dev.alkolhar.servdesk.ticket;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.linkTo;

import java.util.ArrayList;
import java.util.List;
import org.springframework.hateoas.Link;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Renders a ticket's {@code tasks} and its {@code action:*} links for the
 * ticket assemblers (ADR-0003: the server is the single judge of role and
 * state; a link exists only for what this caller may do now). For the MVP,
 * "may" means "is an Agent"; assignee- and team-based rules come with task
 * assignment (#113) and change only this class.
 * <p>
 * One task query per ticket rendered — fine at MVP page sizes; a listing that
 * needs more batches the query here.
 */
@Component
public class TicketTaskModels {

	/** Link relation prefix: {@code action:resolve}, {@code action:cancel}, … */
	public static final String ACTION_REL_PREFIX = "action:";

	public record Rendered(List<TicketTaskModel> tasks, List<Link> actionLinks) {
	}

	private final TicketTasks ticketTasks;

	public TicketTaskModels(TicketTasks ticketTasks) {
		this.ticketTasks = ticketTasks;
	}

	public Rendered render(Long ticketId) {
		boolean mayAct = callerIsAgent();
		List<TicketTaskModel> tasks = new ArrayList<>();
		List<Link> actionLinks = new ArrayList<>();
		for (TicketTasks.OpenTask open : ticketTasks.openTasks(ticketId)) {
			TicketTaskModel task = new TicketTaskModel();
			task.setKey(open.key());
			task.setAssigneeId(open.assigneeId());
			task.setCreatedAt(open.createdAt());
			if (mayAct) {
				for (String action : open.actions()) {
					Link link = linkTo(TicketActionController.class, ticketId).slash(action)
							.withRel(ACTION_REL_PREFIX + action);
					task.add(link);
					actionLinks.add(link);
				}
			}
			tasks.add(task);
		}
		return new Rendered(tasks, actionLinks);
	}

	private static boolean callerIsAgent() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return authentication != null && authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority)
				.anyMatch("ROLE_AGENT"::equals);
	}

}
