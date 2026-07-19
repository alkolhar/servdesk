package dev.alkolhar.servdesk.ticket.problem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import dev.alkolhar.servdesk.common.exception.NotFoundException;
import dev.alkolhar.servdesk.directory.Person;
import dev.alkolhar.servdesk.ticket.Ticket;
import dev.alkolhar.servdesk.ticket.TicketRepository;
import dev.alkolhar.servdesk.ticket.TicketStatus;
import dev.alkolhar.servdesk.ticket.event.TicketStatusChangedEvent;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Exercises {@code AbstractTicketSubtypeCommandService} through the simplest
 * concrete subtype (Problem carries no field of its own beyond the shared ones)
 * — {@code ProblemControllerTest}/the shared abstract integration-test base
 * already cover this end-to-end; these pin down the internal
 * resolvedAt/closedAt derivation and display-number assignment that the HTTP
 * level can't easily observe.
 */
@ExtendWith(MockitoExtension.class)
class ProblemCommandServiceTest {

	@Mock
	private ProblemRepository problemRepository;

	@Mock
	private ProblemQueryService problemQueryService;

	@Mock
	private TicketRepository ticketRepository;

	@Mock
	private EntityManager entityManager;

	@Mock
	private ApplicationEventPublisher events;

	private ProblemCommandService commandService;

	@BeforeEach
	void setUp() {
		commandService = new ProblemCommandService(problemRepository, problemQueryService, ticketRepository,
				entityManager, events);
	}

	private void stubSavesToReturnTheirArgument() {
		when(ticketRepository.save(any(Ticket.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(problemRepository.save(any(Problem.class))).thenAnswer(invocation -> invocation.getArgument(0));
	}

	@Test
	void createAssignsAPrbPrefixedDisplayNumberFromItsOwnSequence() {
		stubSavesToReturnTheirArgument();
		stubSequence("problem_number_seq", 1007L);
		when(entityManager.getReference(Person.class, 3L)).thenReturn(mock(Person.class));

		Problem saved = commandService
				.create(new ProblemCreateRequest("Printer on fire", null, null, null, 3L, null, null));

		assertThat(saved.getDisplayNumber()).isEqualTo("PRB-001007");
	}

	@Test
	void updateDoesNotTouchResolvedAtWhenStatusIsUnchanged() {
		stubSavesToReturnTheirArgument();
		Problem existing = existingProblem(TicketStatus.IN_PROGRESS);
		when(problemQueryService.findById(5L)).thenReturn(existing);
		when(entityManager.getReference(Person.class, 3L)).thenReturn(mock(Person.class));

		commandService.update(5L, new ProblemUpdateRequest(TicketStatus.IN_PROGRESS, "Printer on fire", null, null,
				null, 3L, null, null));

		assertThat(existing.getTicket().getResolvedAt()).isNull();
		verifyNoInteractions(events);
	}

	@Test
	void updateSetsResolvedAtOnlyWhenStatusActuallyTransitionsToResolved() {
		stubSavesToReturnTheirArgument();
		Problem existing = existingProblem(TicketStatus.IN_PROGRESS);
		when(problemQueryService.findById(5L)).thenReturn(existing);
		when(entityManager.getReference(Person.class, 3L)).thenReturn(mock(Person.class));

		commandService.update(5L,
				new ProblemUpdateRequest(TicketStatus.RESOLVED, "Printer on fire", null, null, null, 3L, null, null));

		assertThat(existing.getTicket().getResolvedAt()).isNotNull();
		ArgumentCaptor<TicketStatusChangedEvent> captor = ArgumentCaptor.forClass(TicketStatusChangedEvent.class);
		verify(events).publishEvent(captor.capture());
		assertThat(captor.getValue().previousStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
		assertThat(captor.getValue().newStatus()).isEqualTo(TicketStatus.RESOLVED);
	}

	@Test
	void updatePreservesResolvedAtWhenProgressingFromResolvedToClosed() {
		stubSavesToReturnTheirArgument();
		Problem existing = existingProblem(TicketStatus.RESOLVED);
		Instant previouslyResolvedAt = Instant.parse("2026-01-01T00:00:00Z");
		existing.getTicket().setResolvedAt(previouslyResolvedAt);
		when(problemQueryService.findById(5L)).thenReturn(existing);
		when(entityManager.getReference(Person.class, 3L)).thenReturn(mock(Person.class));

		commandService.update(5L,
				new ProblemUpdateRequest(TicketStatus.CLOSED, "Printer on fire", null, null, null, 3L, null, null));

		assertThat(existing.getTicket().getResolvedAt()).isEqualTo(previouslyResolvedAt);
		assertThat(existing.getTicket().getClosedAt()).isNotNull();
	}

	@Test
	void updateClearsResolvedAtWhenReopenedToAnEarlierStatus() {
		stubSavesToReturnTheirArgument();
		Problem existing = existingProblem(TicketStatus.RESOLVED);
		existing.getTicket().setResolvedAt(Instant.now());
		when(problemQueryService.findById(5L)).thenReturn(existing);
		when(entityManager.getReference(Person.class, 3L)).thenReturn(mock(Person.class));

		commandService.update(5L, new ProblemUpdateRequest(TicketStatus.IN_PROGRESS, "Printer on fire", null, null,
				null, 3L, null, null));

		assertThat(existing.getTicket().getResolvedAt()).isNull();
	}

	@Test
	void deleteSoftDeletesBothTheSubtypeAndTheSharedTicketRow() {
		Problem existing = existingProblem(TicketStatus.OPEN);
		when(problemQueryService.findById(9L)).thenReturn(existing);

		commandService.delete(9L);

		verify(problemRepository).delete(existing);
		verify(ticketRepository).delete(existing.getTicket());
	}

	@Test
	void deleteNeverTouchesRepositoriesWhenTheProblemDoesNotExist() {
		when(problemQueryService.findById(999L)).thenThrow(new NotFoundException("Problem 999 not found"));

		assertThatThrownBy(() -> commandService.delete(999L)).isInstanceOf(NotFoundException.class);

		verify(problemRepository, never()).delete(any());
		verify(ticketRepository, never()).delete(any());
	}

	private Problem existingProblem(TicketStatus status) {
		Ticket ticket = new Ticket();
		ticket.setStatus(status);
		ReflectionTestUtils.setField(ticket, "id", 5L);
		Problem problem = new Problem();
		problem.setTicket(ticket);
		ReflectionTestUtils.setField(problem, "id", 5L);
		return problem;
	}

	private void stubSequence(String sequenceName, long value) {
		Query query = mock(Query.class);
		when(query.getSingleResult()).thenReturn(value);
		when(entityManager.createNativeQuery("SELECT NEXTVAL(" + sequenceName + ")")).thenReturn(query);
	}
}
