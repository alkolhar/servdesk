package dev.alkolhar.servdesk.ticket;

import static org.assertj.core.api.Assertions.assertThat;

import dev.alkolhar.servdesk.TestcontainersConfiguration;
import dev.alkolhar.servdesk.directory.Person;
import dev.alkolhar.servdesk.directory.PersonRepository;
import dev.alkolhar.servdesk.directory.PersonRole;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Proves a bare {@link Ticket} — now holding only the fields shared across
 * every subtype, with {@code type}/{@code ticketNumber} removed per ADR-0001 —
 * still round-trips through a real PostgreSQL instance via
 * {@link TicketRepository}.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class TicketRepositoryTest {

	@Autowired
	private TicketRepository ticketRepository;

	@Autowired
	private PersonRepository personRepository;

	@Test
	void persistsAndReloadsABareTicket() {
		Person requester = new Person();
		requester.setRole(PersonRole.CUSTOMER);
		requester.setName("Carla Customer");
		requester.setEmail("carla@example.com");
		requester = personRepository.save(requester);

		Ticket ticket = new Ticket();
		ticket.setSubject("Printer on fire");
		ticket.setDescription("Smoke coming from the third floor printer.");
		ticket.setRequester(requester);
		Long id = ticketRepository.save(ticket).getId();

		Optional<Ticket> reloaded = ticketRepository.findById(id);

		assertThat(reloaded).isPresent();
		assertThat(reloaded.get().getSubject()).isEqualTo("Printer on fire");
		assertThat(reloaded.get().getStatus()).isEqualTo(TicketStatus.OPEN);
		assertThat(reloaded.get().getRequester().getId()).isEqualTo(requester.getId());
		assertThat(reloaded.get().getCreatedAt()).isNotNull();
	}
}
