package dev.alkolhar.servdesk.ticket;

import dev.alkolhar.servdesk.common.exception.NotFoundException;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class TicketQueryService {

	private final TicketRepository ticketRepository;

	public TicketQueryService(TicketRepository ticketRepository) {
		this.ticketRepository = ticketRepository;
	}

	public Page<Ticket> findAll(@Nullable TicketStatus status, @Nullable TicketType type, Pageable pageable) {
		return ticketRepository.findByOptionalFilters(status, type, pageable);
	}

	public Ticket findById(Long id) {
		return ticketRepository.findById(id).orElseThrow(() -> new NotFoundException("Ticket " + id + " not found"));
	}
}
