package dev.alkolhar.servdesk.ticket.overview;

import dev.alkolhar.servdesk.common.exception.NotFoundException;
import dev.alkolhar.servdesk.ticket.Ticket;
import dev.alkolhar.servdesk.ticket.TicketRepository;
import dev.alkolhar.servdesk.ticket.TicketStatus;
import dev.alkolhar.servdesk.ticket.change.ChangeRepository;
import dev.alkolhar.servdesk.ticket.incident.IncidentRepository;
import dev.alkolhar.servdesk.ticket.problem.ProblemRepository;
import dev.alkolhar.servdesk.ticket.servicerequest.ServiceRequestRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Read-only queries over the shared ticket table, across all four subtypes
 * (issue #30). Row-level ownership follows
 * {@code AbstractTicketSubtypeQueryService}: a Customer only sees tickets they
 * requested, and a foreign ticket answers 404, indistinguishable from a missing
 * one.
 * <p>
 * Type/display number live on each subtype's own table (ADR-0001), so a page of
 * tickets is joined up here with one {@code findAllById} batch per subtype
 * repository — four queries per page, never per row. Every live ticket has
 * exactly one live subtype row (creation always writes both; deletion
 * soft-deletes both), so an unmatched ticket is data corruption worth failing
 * loudly on, not a 404.
 */
@Service
public class TicketQueryService {

	private final TicketRepository ticketRepository;
	private final IncidentRepository incidentRepository;
	private final ProblemRepository problemRepository;
	private final ChangeRepository changeRepository;
	private final ServiceRequestRepository serviceRequestRepository;

	public TicketQueryService(TicketRepository ticketRepository, IncidentRepository incidentRepository,
			ProblemRepository problemRepository, ChangeRepository changeRepository,
			ServiceRequestRepository serviceRequestRepository) {
		this.ticketRepository = ticketRepository;
		this.incidentRepository = incidentRepository;
		this.problemRepository = problemRepository;
		this.changeRepository = changeRepository;
		this.serviceRequestRepository = serviceRequestRepository;
	}

	public Page<TicketOverview> findAll(@Nullable TicketStatus status, @Nullable Long requesterId,
			@Nullable Long assigneeId, @Nullable Long teamId, @Nullable Long categoryId, @Nullable Long priorityId,
			@Nullable String attrKey, @Nullable String attrValue, Long callerId, boolean callerIsAgent,
			Pageable pageable) {
		if (attrKey != null && attrValue == null) {
			throw new IllegalArgumentException("attrValue is required when attrKey is given");
		}
		Long requesterScope = callerIsAgent ? null : callerId;
		Page<Ticket> page = attrKey == null
				? ticketRepository.findVisible(status, requesterId, assigneeId, teamId, categoryId, priorityId,
						requesterScope, pageable)
				: ticketRepository.findVisibleByAttribute(status, requesterId, assigneeId, teamId, categoryId,
						priorityId, requesterScope, attrKey, attrValue, pageable);
		Map<Long, TicketOverview> resolved = resolveSubtypes(page.getContent());
		return page.map(ticket -> overviewOf(ticket, resolved));
	}

	public TicketOverview findByIdVisibleTo(Long id, Long callerId, boolean callerIsAgent) {
		Ticket ticket = ticketRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Ticket " + id + " not found"));
		if (!callerIsAgent && !callerId.equals(ticket.getRequester().getId())) {
			throw new NotFoundException("Ticket " + id + " not found");
		}
		return overviewOf(ticket, resolveSubtypes(List.of(ticket)));
	}

	private Map<Long, TicketOverview> resolveSubtypes(List<Ticket> tickets) {
		Map<Long, Ticket> byId = new HashMap<>();
		tickets.forEach(ticket -> byId.put(ticket.getId(), ticket));
		Map<Long, TicketOverview> resolved = new HashMap<>();
		incidentRepository.findAllById(byId.keySet()).forEach(incident -> resolved.put(incident.getId(),
				new TicketOverview(byId.get(incident.getId()), TicketType.INCIDENT, incident.getDisplayNumber())));
		problemRepository.findAllById(byId.keySet()).forEach(problem -> resolved.put(problem.getId(),
				new TicketOverview(byId.get(problem.getId()), TicketType.PROBLEM, problem.getDisplayNumber())));
		changeRepository.findAllById(byId.keySet()).forEach(change -> resolved.put(change.getId(),
				new TicketOverview(byId.get(change.getId()), TicketType.CHANGE, change.getDisplayNumber())));
		serviceRequestRepository.findAllById(byId.keySet())
				.forEach(serviceRequest -> resolved.put(serviceRequest.getId(),
						new TicketOverview(byId.get(serviceRequest.getId()), TicketType.SERVICE_REQUEST,
								serviceRequest.getDisplayNumber())));
		return resolved;
	}

	private TicketOverview overviewOf(Ticket ticket, Map<Long, TicketOverview> resolved) {
		TicketOverview overview = resolved.get(ticket.getId());
		if (overview == null) {
			throw new IllegalStateException("Ticket " + ticket.getId()
					+ " has no live subtype row — creation always writes both, deletion soft-deletes both");
		}
		return overview;
	}
}
