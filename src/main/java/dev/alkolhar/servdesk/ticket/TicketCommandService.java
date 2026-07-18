package dev.alkolhar.servdesk.ticket;

import dev.alkolhar.servdesk.classification.Category;
import dev.alkolhar.servdesk.classification.Priority;
import dev.alkolhar.servdesk.common.BaseEntity;
import dev.alkolhar.servdesk.directory.Person;
import dev.alkolhar.servdesk.directory.Team;
import dev.alkolhar.servdesk.ticket.event.TicketCreatedEvent;
import dev.alkolhar.servdesk.ticket.event.TicketStatusChangedEvent;
import jakarta.persistence.EntityManager;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TicketCommandService {

	private final TicketRepository ticketRepository;
	private final TicketQueryService ticketQueryService;
	private final EntityManager entityManager;
	private final ApplicationEventPublisher events;

	public TicketCommandService(TicketRepository ticketRepository, TicketQueryService ticketQueryService,
			EntityManager entityManager, ApplicationEventPublisher events) {
		this.ticketRepository = ticketRepository;
		this.ticketQueryService = ticketQueryService;
		this.entityManager = entityManager;
		this.events = events;
	}

	@Transactional
	public Ticket create(TicketCreateRequest request) {
		Ticket ticket = new Ticket();
		ticket.setType(request.type());
		ticket.setSubject(request.subject());
		ticket.setDescription(request.description());
		ticket.setCategory(reference(Category.class, request.categoryId()));
		ticket.setPriority(reference(Priority.class, request.priorityId()));
		ticket.setRequester(reference(Person.class, request.requesterId()));
		ticket.setAssignee(reference(Person.class, request.assigneeId()));
		ticket.setTeam(reference(Team.class, request.teamId()));
		ticket.setTicketNumber("TCK-%06d".formatted(nextTicketNumber()));
		Ticket saved = ticketRepository.save(ticket);
		events.publishEvent(new TicketCreatedEvent(saved.getId(), saved.getTicketNumber()));
		return saved;
	}

	public Ticket update(Long id, TicketUpdateRequest request) {
		Ticket existing = ticketQueryService.findById(id);
		TicketStatus previousStatus = existing.getStatus();
		existing.setType(request.type());
		existing.setStatus(request.status());
		existing.setSubject(request.subject());
		existing.setDescription(request.description());
		existing.setCategory(reference(Category.class, request.categoryId()));
		existing.setPriority(reference(Priority.class, request.priorityId()));
		existing.setRequester(reference(Person.class, request.requesterId()));
		existing.setAssignee(reference(Person.class, request.assigneeId()));
		existing.setTeam(reference(Team.class, request.teamId()));
		existing.setResolvedAt(request.resolvedAt());
		existing.setClosedAt(request.closedAt());
		Ticket saved = ticketRepository.save(existing);
		if (previousStatus != saved.getStatus()) {
			events.publishEvent(new TicketStatusChangedEvent(saved.getId(), previousStatus, saved.getStatus()));
		}
		return saved;
	}

	public void delete(Long id) {
		ticketRepository.delete(ticketQueryService.findById(id));
	}

	/**
	 * Drawn from {@code ticket_number_seq} rather than the generated id, since the
	 * id is only known after insert but {@code ticket_number} is NOT NULL and must
	 * be set beforehand.
	 */
	private long nextTicketNumber() {
		return ((Number) entityManager.createNativeQuery("SELECT NEXTVAL(ticket_number_seq)").getSingleResult())
				.longValue();
	}

	/**
	 * Requests carry related entities (category/priority/requester/assignee/team)
	 * as plain ids; resolve each to a managed proxy rather than loading the full
	 * row.
	 */
	private <T extends BaseEntity> @Nullable T reference(Class<T> type, @Nullable Long id) {
		return id == null ? null : entityManager.getReference(type, id);
	}
}
