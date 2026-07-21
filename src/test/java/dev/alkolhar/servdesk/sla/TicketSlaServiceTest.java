package dev.alkolhar.servdesk.sla;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import dev.alkolhar.servdesk.classification.Priority;
import dev.alkolhar.servdesk.ticket.Ticket;
import dev.alkolhar.servdesk.ticket.TicketStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Pins down the deadline arithmetic and clock-pausing that HTTP-level tests
 * can't time-travel: derivation on create and on an actual priority change, no
 * recompute on unrelated updates, PENDING recording/shifting.
 */
@ExtendWith(MockitoExtension.class)
class TicketSlaServiceTest {

	@Mock
	private SlaPolicyRepository policyRepository;

	private TicketSlaService slaService;

	@BeforeEach
	void setUp() {
		slaService = new TicketSlaService(policyRepository);
	}

	@Test
	void derivesBothDeadlinesFromThePriorityPolicyOnCreate() {
		when(policyRepository.findByPriorityId(7L)).thenReturn(Optional.of(policy(30, 240)));
		Ticket ticket = ticketWithPriority(7L);

		slaService.applyOnWrite(ticket, null, null);

		assertThat(ticket.getRespondBy()).isNotNull();
		assertThat(ticket.getResolveBy()).isNotNull();
		assertThat(Duration.between(ticket.getRespondBy(), ticket.getResolveBy())).isEqualTo(Duration.ofMinutes(210));
	}

	@Test
	void leavesDeadlinesNullWithoutAPriorityOrPolicy() {
		Ticket withoutPriority = new Ticket();
		slaService.applyOnWrite(withoutPriority, null, null);
		assertThat(withoutPriority.getRespondBy()).isNull();
		assertThat(withoutPriority.getResolveBy()).isNull();

		when(policyRepository.findByPriorityId(7L)).thenReturn(Optional.empty());
		Ticket withoutPolicy = ticketWithPriority(7L);
		slaService.applyOnWrite(withoutPolicy, null, null);
		assertThat(withoutPolicy.getRespondBy()).isNull();
	}

	@Test
	void recomputesOnlyWhenThePriorityActuallyChanged() {
		when(policyRepository.findByPriorityId(7L)).thenReturn(Optional.of(policy(30, 240)));
		Ticket ticket = ticketWithPriority(7L);
		slaService.applyOnWrite(ticket, null, null);
		Instant originalRespondBy = ticket.getRespondBy();

		// unrelated update, same priority: untouched
		slaService.applyOnWrite(ticket, TicketStatus.OPEN, 7L);
		assertThat(ticket.getRespondBy()).isEqualTo(originalRespondBy);

		// actual change: recomputed from the new policy
		when(policyRepository.findByPriorityId(8L)).thenReturn(Optional.of(policy(10, 60)));
		setPriorityId(ticket, 8L);
		slaService.applyOnWrite(ticket, TicketStatus.OPEN, 7L);
		assertThat(Duration.between(ticket.getRespondBy(), ticket.getResolveBy())).isEqualTo(Duration.ofMinutes(50));
	}

	@Test
	void enteringPendingRecordsPendingSinceAndLeavingItShiftsBothDeadlines() {
		when(policyRepository.findByPriorityId(7L)).thenReturn(Optional.of(policy(30, 240)));
		Ticket ticket = ticketWithPriority(7L);
		slaService.applyOnWrite(ticket, null, null);
		Instant respondBy = ticket.getRespondBy();

		ticket.setStatus(TicketStatus.PENDING);
		slaService.applyOnWrite(ticket, TicketStatus.OPEN, 7L);
		assertThat(ticket.getPendingSince()).isNotNull();
		assertThat(ticket.getRespondBy()).isEqualTo(respondBy);

		// simulate an hour spent pending
		ticket.setPendingSince(ticket.getPendingSince().minus(Duration.ofHours(1)));
		Instant backdatedPendingSince = ticket.getPendingSince();
		ticket.setStatus(TicketStatus.IN_PROGRESS);
		slaService.applyOnWrite(ticket, TicketStatus.PENDING, 7L);

		assertThat(ticket.getPendingSince()).isNull();
		assertThat(Duration.between(respondBy, ticket.getRespondBy())).isGreaterThanOrEqualTo(Duration.ofHours(1));
		assertThat(backdatedPendingSince).isNotNull();
	}

	@Test
	void recordsTheFirstAgentResponseOnceAndNeverOverwritesIt() {
		Ticket ticket = new Ticket();
		slaService.recordAgentResponse(ticket);
		Instant first = ticket.getFirstRespondedAt();
		assertThat(first).isNotNull();

		slaService.recordAgentResponse(ticket);
		assertThat(ticket.getFirstRespondedAt()).isEqualTo(first);
	}

	private SlaPolicy policy(int responseMinutes, int resolutionMinutes) {
		SlaPolicy policy = new SlaPolicy();
		policy.setResponseMinutes(responseMinutes);
		policy.setResolutionMinutes(resolutionMinutes);
		return policy;
	}

	private Ticket ticketWithPriority(Long priorityId) {
		Ticket ticket = new Ticket();
		setPriorityId(ticket, priorityId);
		return ticket;
	}

	private void setPriorityId(Ticket ticket, Long priorityId) {
		Priority priority = new Priority();
		ReflectionTestUtils.setField(priority, "id", priorityId);
		ticket.setPriority(priority);
	}
}
