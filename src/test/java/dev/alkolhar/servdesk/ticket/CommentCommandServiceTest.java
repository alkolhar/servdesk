package dev.alkolhar.servdesk.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.alkolhar.servdesk.common.exception.ForbiddenException;
import dev.alkolhar.servdesk.common.exception.NotFoundException;
import dev.alkolhar.servdesk.directory.Person;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Pure Mockito unit test for the one enforcement rule that's new in this
 * codebase: a Customer's {@code internal=true} comment is rejected — mirrors
 * the shape of
 * {@code PersonCommandServiceTest#createInitialAgentRejectsWhenSetupAlreadyCompleted},
 * asserting the rejection happens before the repository save is ever reached.
 */
@ExtendWith(MockitoExtension.class)
class CommentCommandServiceTest {

	@Mock
	private TicketCommentRepository commentRepository;

	@Mock
	private TicketRepository ticketRepository;

	@Mock
	private EntityManager entityManager;

	private CommentCommandService commandService;

	@BeforeEach
	void setUp() {
		commandService = new CommentCommandService(commentRepository, ticketRepository, entityManager);
	}

	@Test
	void rejectsACustomersAttemptToMarkTheirOwnCommentInternalBeforeSaving() {
		assertThatThrownBy(() -> commandService.create(1L, new CommentCreateRequest("Please help", true), 3L, false))
				.isInstanceOf(ForbiddenException.class);

		verify(commentRepository, never()).save(any());
		verify(ticketRepository, never()).findById(any());
	}

	@Test
	void allowsAnAgentToMarkACommentInternal() {
		Ticket ticket = new Ticket();
		ReflectionTestUtils.setField(ticket, "id", 1L);
		when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
		when(entityManager.getReference(Person.class, 3L)).thenReturn(mock(Person.class));
		when(commentRepository.save(any(TicketComment.class))).thenAnswer(invocation -> invocation.getArgument(0));

		TicketComment saved = commandService.create(1L, new CommentCreateRequest("Working on it", true), 3L, true);

		assertThat(saved.isInternal()).isTrue();
	}

	@Test
	void doesNotRestrictANonInternalCommentToEitherRole() {
		Ticket ticket = new Ticket();
		ReflectionTestUtils.setField(ticket, "id", 1L);
		when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));
		when(entityManager.getReference(Person.class, 3L)).thenReturn(mock(Person.class));
		when(commentRepository.save(any(TicketComment.class))).thenAnswer(invocation -> invocation.getArgument(0));

		TicketComment saved = commandService.create(1L, new CommentCreateRequest("Thanks!", false), 3L, false);

		assertThat(saved.isInternal()).isFalse();
	}

	@Test
	void createThrowsNotFoundWhenTheTicketDoesNotExist() {
		when(ticketRepository.findById(404L)).thenReturn(Optional.empty());

		assertThatThrownBy(
				() -> commandService.create(404L, new CommentCreateRequest("Anyone there?", false), 3L, false))
				.isInstanceOf(NotFoundException.class);

		verify(commentRepository, never()).save(any());
	}
}
