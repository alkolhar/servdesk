package dev.alkolhar.servdesk.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import dev.alkolhar.servdesk.ticket.event.TicketStatusChangedEvent;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * The single writer of {@code Ticket.status} (ADR-0004), with every
 * collaborator mocked. Which transitions exist is the process's business
 * ({@code IncidentLifecycleTest} drives a real engine); this pins down what a
 * transition <i>means</i>, stage by stage.
 */
@ExtendWith(MockitoExtension.class)
class TicketLifecycleTest {

	@Mock
	private TicketRepository ticketRepository;

	@Mock
	private SlaHooks slaHooks;

	@Mock
	private ApplicationEventPublisher events;

	private TicketLifecycle lifecycle;

	@BeforeEach
	void setUp() {
		lifecycle = new TicketLifecycle(ticketRepository, slaHooks, events);
	}

	private static Ticket ticket(TicketStatus status) {
		Ticket ticket = new Ticket();
		ticket.setStatus(status);
		ReflectionTestUtils.setField(ticket, "id", 7L);
		return ticket;
	}

	@Test
	void resolvingStampsResolvedAt() {
		Ticket ticket = ticket(TicketStatus.IN_PROGRESS);

		lifecycle.applyStatus(ticket, TicketStatus.RESOLVED);

		assertThat(ticket.getStatus()).isEqualTo(TicketStatus.RESOLVED);
		assertThat(ticket.getResolvedAt()).isNotNull();
		assertThat(ticket.getClosedAt()).isNull();
	}

	@Test
	void reopeningClearsResolvedAt() {
		Ticket ticket = ticket(TicketStatus.RESOLVED);
		ticket.setResolvedAt(Instant.parse("2026-01-01T00:00:00Z"));

		lifecycle.applyStatus(ticket, TicketStatus.IN_PROGRESS);

		assertThat(ticket.getResolvedAt()).isNull();
	}

	@Test
	void closingAResolvedTicketKeepsItsResolvedAt() {
		Ticket ticket = ticket(TicketStatus.RESOLVED);
		Instant resolvedAt = Instant.parse("2026-01-01T00:00:00Z");
		ticket.setResolvedAt(resolvedAt);

		lifecycle.applyStatus(ticket, TicketStatus.CLOSED);

		assertThat(ticket.getResolvedAt()).isEqualTo(resolvedAt);
		assertThat(ticket.getClosedAt()).isNotNull();
	}

	/**
	 * {@code cancel} (ADR-0008): closed, never resolved — resolution metrics stay
	 * honest.
	 */
	@Test
	void closingAnUnresolvedTicketLeavesResolvedAtEmpty() {
		Ticket ticket = ticket(TicketStatus.IN_PROGRESS);

		lifecycle.applyStatus(ticket, TicketStatus.CLOSED);

		assertThat(ticket.getClosedAt()).isNotNull();
		assertThat(ticket.getResolvedAt()).isNull();
	}

	@Test
	void everyChangeGoesThroughTheSlaHooksWithTheExplicitPreviousStatus() {
		Ticket ticket = ticket(TicketStatus.IN_PROGRESS);

		lifecycle.applyStatus(ticket, TicketStatus.PENDING);

		// no priority on the ticket, and none changing: only the pause is the SLA's
		// business
		verify(slaHooks).applyOnWrite(ticket, TicketStatus.IN_PROGRESS, null);
		verify(events).publishEvent(new TicketStatusChangedEvent(7L, TicketStatus.IN_PROGRESS, TicketStatus.PENDING));
	}

	/**
	 * A process's first stage enters OPEN on a ticket that is already OPEN (the
	 * column's fallback default): nothing happened, so nothing is reported.
	 */
	@Test
	void enteringTheCurrentStatusChangesAndPublishesNothing() {
		Ticket ticket = ticket(TicketStatus.OPEN);

		lifecycle.applyStatus(ticket, TicketStatus.OPEN);

		assertThat(ticket.getClosedAt()).isNull();
		verifyNoInteractions(slaHooks, events);
	}

	@Test
	void theProcessEntryPointLoadsAndSavesTheTicketByItsBusinessKey() {
		Ticket ticket = ticket(TicketStatus.OPEN);
		when(ticketRepository.findById(7L)).thenReturn(Optional.of(ticket));

		lifecycle.enterStatus(7L, TicketStatus.IN_PROGRESS);

		assertThat(ticket.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
		verify(ticketRepository).save(ticket);
	}

	/**
	 * A running process for a ticket that isn't there is a broken invariant, not
	 * bad input.
	 */
	@Test
	void theProcessEntryPointRefusesATicketThatDoesNotExist() {
		when(ticketRepository.findById(7L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> lifecycle.enterStatus(7L, TicketStatus.IN_PROGRESS))
				.isInstanceOf(IllegalStateException.class);
	}

}
