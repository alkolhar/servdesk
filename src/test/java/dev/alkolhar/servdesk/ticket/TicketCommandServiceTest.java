package dev.alkolhar.servdesk.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.alkolhar.servdesk.classification.Category;
import dev.alkolhar.servdesk.classification.Priority;
import dev.alkolhar.servdesk.directory.Person;
import dev.alkolhar.servdesk.directory.Team;
import dev.alkolhar.servdesk.ticket.event.TicketCreatedEvent;
import dev.alkolhar.servdesk.ticket.event.TicketStatusChangedEvent;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Pure Mockito unit tests: no Spring context, no database.
 * {@link TicketControllerTest} already covers this service end-to-end — these
 * tests exist for the one piece of logic that's easy to get subtly wrong and
 * hard to observe from the outside: {@code update()} must publish
 * {@link TicketStatusChangedEvent} only when the status actually changes, not
 * on every save.
 */
@ExtendWith(MockitoExtension.class)
class TicketCommandServiceTest {

	@Mock
	private TicketRepository ticketRepository;

	@Mock
	private TicketQueryService ticketQueryService;

	@Mock
	private EntityManager entityManager;

	@Mock
	private ApplicationEventPublisher events;

	private TicketCommandService commandService;

	@BeforeEach
	void setUp() {
		commandService = new TicketCommandService(ticketRepository, ticketQueryService, entityManager, events);
	}

	private void stubTicketNumberSequence(long nextValue) {
		Query query = mock(Query.class);
		when(entityManager.createNativeQuery(anyString())).thenReturn(query);
		when(query.getSingleResult()).thenReturn(nextValue);
	}

	private void stubSaveToAssignId(long id) {
		when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> {
			Ticket ticket = invocation.getArgument(0);
			ReflectionTestUtils.setField(ticket, "id", id);
			return ticket;
		});
	}

	@Test
	void createResolvesAllReferencesWhenIdsPresent() {
		stubTicketNumberSequence(1000L);
		stubSaveToAssignId(1L);
		Category category = new Category();
		Priority priority = new Priority();
		Person requester = new Person();
		Person assignee = new Person();
		Team team = new Team();
		when(entityManager.getReference(Category.class, 2L)).thenReturn(category);
		when(entityManager.getReference(Priority.class, 3L)).thenReturn(priority);
		when(entityManager.getReference(Person.class, 4L)).thenReturn(requester);
		when(entityManager.getReference(Person.class, 5L)).thenReturn(assignee);
		when(entityManager.getReference(Team.class, 6L)).thenReturn(team);

		Ticket saved = commandService
				.create(new TicketCreateRequest(TicketType.INCIDENT, "Subject", null, 2L, 3L, 4L, 5L, 6L));

		assertThat(saved.getCategory()).isSameAs(category);
		assertThat(saved.getPriority()).isSameAs(priority);
		assertThat(saved.getRequester()).isSameAs(requester);
		assertThat(saved.getAssignee()).isSameAs(assignee);
		assertThat(saved.getTeam()).isSameAs(team);
	}

	@Test
	void createLeavesOptionalReferencesNullWhenIdsAbsent() {
		stubTicketNumberSequence(1000L);
		stubSaveToAssignId(1L);
		Person requester = new Person();
		when(entityManager.getReference(Person.class, 4L)).thenReturn(requester);

		Ticket saved = commandService
				.create(new TicketCreateRequest(TicketType.INCIDENT, "Subject", null, null, null, 4L, null, null));

		assertThat(saved.getCategory()).isNull();
		assertThat(saved.getPriority()).isNull();
		assertThat(saved.getAssignee()).isNull();
		assertThat(saved.getTeam()).isNull();
	}

	@Test
	void createAssignsTicketNumberFromSequence() {
		stubTicketNumberSequence(1042L);
		stubSaveToAssignId(1L);
		when(entityManager.getReference(Person.class, 4L)).thenReturn(new Person());

		Ticket saved = commandService
				.create(new TicketCreateRequest(TicketType.INCIDENT, "Subject", null, null, null, 4L, null, null));

		assertThat(saved.getTicketNumber()).isEqualTo("TCK-001042");
	}

	@Test
	void createPublishesTicketCreatedEvent() {
		stubTicketNumberSequence(1000L);
		stubSaveToAssignId(77L);
		when(entityManager.getReference(Person.class, 4L)).thenReturn(new Person());

		commandService
				.create(new TicketCreateRequest(TicketType.INCIDENT, "Subject", null, null, null, 4L, null, null));

		ArgumentCaptor<TicketCreatedEvent> captor = ArgumentCaptor.forClass(TicketCreatedEvent.class);
		verify(events).publishEvent(captor.capture());
		assertThat(captor.getValue().ticketId()).isEqualTo(77L);
		assertThat(captor.getValue().ticketNumber()).isEqualTo("TCK-001000");
	}

	@Test
	void updateDoesNotPublishStatusChangedEventWhenStatusIsUnchanged() {
		Ticket existing = existingTicketWithStatus(TicketStatus.OPEN);
		when(ticketQueryService.findById(1L)).thenReturn(existing);
		when(ticketRepository.save(existing)).thenReturn(existing);
		when(entityManager.getReference(Person.class, 4L)).thenReturn(new Person());

		commandService.update(1L, new TicketUpdateRequest(TicketType.INCIDENT, TicketStatus.OPEN, "Subject", null, null,
				null, 4L, null, null, null, null));

		verify(events, never()).publishEvent(any(TicketStatusChangedEvent.class));
	}

	@Test
	void updatePublishesStatusChangedEventWithPreviousAndNewStatusWhenStatusChanges() {
		Ticket existing = existingTicketWithStatus(TicketStatus.OPEN);
		ReflectionTestUtils.setField(existing, "id", 5L);
		when(ticketQueryService.findById(5L)).thenReturn(existing);
		when(ticketRepository.save(existing)).thenReturn(existing);
		when(entityManager.getReference(Person.class, 4L)).thenReturn(new Person());

		commandService.update(5L, new TicketUpdateRequest(TicketType.INCIDENT, TicketStatus.RESOLVED, "Subject", null,
				null, null, 4L, null, null, null, null));

		ArgumentCaptor<TicketStatusChangedEvent> captor = ArgumentCaptor.forClass(TicketStatusChangedEvent.class);
		verify(events).publishEvent(captor.capture());
		assertThat(captor.getValue().ticketId()).isEqualTo(5L);
		assertThat(captor.getValue().previousStatus()).isEqualTo(TicketStatus.OPEN);
		assertThat(captor.getValue().newStatus()).isEqualTo(TicketStatus.RESOLVED);
	}

	@Test
	void deleteDelegatesToRepositoryWithTheEntityLoadedByQueryService() {
		Ticket existing = new Ticket();
		when(ticketQueryService.findById(9L)).thenReturn(existing);

		commandService.delete(9L);

		verify(ticketRepository).delete(existing);
	}

	private Ticket existingTicketWithStatus(TicketStatus status) {
		Ticket ticket = new Ticket();
		ticket.setStatus(status);
		return ticket;
	}
}
