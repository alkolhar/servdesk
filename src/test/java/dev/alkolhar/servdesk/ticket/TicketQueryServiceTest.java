package dev.alkolhar.servdesk.ticket;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import dev.alkolhar.servdesk.common.exception.NotFoundException;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class TicketQueryServiceTest {

	@Mock
	private TicketRepository ticketRepository;

	private TicketQueryService queryService;

	@BeforeEach
	void setUp() {
		queryService = new TicketQueryService(ticketRepository);
	}

	@Test
	void findAllDelegatesToRepositoryWithBothOptionalFilters() {
		Pageable pageable = Pageable.ofSize(20);
		Page<Ticket> page = new PageImpl<>(List.of());
		when(ticketRepository.findByOptionalFilters(TicketStatus.OPEN, TicketType.INCIDENT, pageable)).thenReturn(page);

		Page<Ticket> result = queryService.findAll(TicketStatus.OPEN, TicketType.INCIDENT, pageable);

		assertThat(result).isSameAs(page);
	}

	@Test
	void findAllPassesThroughNullFiltersUnchanged() {
		Pageable pageable = Pageable.ofSize(20);
		Page<Ticket> page = new PageImpl<>(List.of());
		when(ticketRepository.findByOptionalFilters(null, null, pageable)).thenReturn(page);

		Page<Ticket> result = queryService.findAll(null, null, pageable);

		assertThat(result).isSameAs(page);
	}

	@Test
	void findByIdThrowsNotFoundExceptionWhenMissing() {
		when(ticketRepository.findById(999L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> queryService.findById(999L)).isInstanceOf(NotFoundException.class)
				.hasMessageContaining("999");
	}

	@Test
	void findByIdReturnsTheTicketWhenPresent() {
		Ticket ticket = new Ticket();
		when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

		assertThat(queryService.findById(1L)).isSameAs(ticket);
	}
}
