package dev.alkolhar.servdesk.directory;

import dev.alkolhar.servdesk.common.exception.ConflictException;
import dev.alkolhar.servdesk.directory.event.PersonCreatedEvent;
import jakarta.persistence.EntityManager;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PersonCommandService {

	private final PersonRepository personRepository;
	private final PersonQueryService personQueryService;
	private final EntityManager entityManager;
	private final PasswordEncoder passwordEncoder;
	private final ApplicationEventPublisher events;

	public PersonCommandService(PersonRepository personRepository, PersonQueryService personQueryService,
			EntityManager entityManager, PasswordEncoder passwordEncoder, ApplicationEventPublisher events) {
		this.personRepository = personRepository;
		this.personQueryService = personQueryService;
		this.entityManager = entityManager;
		this.passwordEncoder = passwordEncoder;
		this.events = events;
	}

	public Person create(PersonCreateRequest request) {
		Person person = new Person();
		person.setRole(request.role());
		person.setName(request.name());
		person.setEmail(request.email());
		person.setPhone(request.phone());
		person.setUsername(request.username());
		person.setPassword(request.password() == null ? null : passwordEncoder.encode(request.password()));
		person.setEnabled(true);
		person.setTeam(resolveTeam(request.teamId()));
		Person saved = personRepository.save(person);
		events.publishEvent(new PersonCreatedEvent(saved.getId(), saved.getRole()));
		return saved;
	}

	public Person update(Long id, PersonUpdateRequest request) {
		Person existing = personQueryService.findById(id);
		existing.setRole(request.role());
		existing.setName(request.name());
		existing.setEmail(request.email());
		existing.setPhone(request.phone());
		existing.setUsername(request.username());
		if (request.password() != null) {
			existing.setPassword(passwordEncoder.encode(request.password()));
		}
		existing.setEnabled(request.enabled());
		existing.setTeam(resolveTeam(request.teamId()));
		return personRepository.save(existing);
	}

	public void delete(Long id) {
		personRepository.delete(personQueryService.findById(id));
	}

	/**
	 * Creates the first agent account. Only succeeds while the database has no
	 * {@link Person} at all.
	 *
	 * @see dev.alkolhar.servdesk.setup.SetupController
	 */
	public Person createInitialAgent(String name, String email, String phone, String username, String password) {
		if (!personQueryService.isSetupRequired()) {
			throw new ConflictException("Setup already completed");
		}
		Person person = new Person();
		person.setRole(PersonRole.AGENT);
		person.setName(name);
		person.setEmail(email);
		person.setPhone(phone);
		person.setUsername(username);
		person.setPassword(passwordEncoder.encode(password));
		person.setEnabled(true);
		Person saved = personRepository.save(person);
		events.publishEvent(new PersonCreatedEvent(saved.getId(), saved.getRole()));
		return saved;
	}

	/**
	 * The request carries {@code teamId} as a plain id rather than a nested object;
	 * resolve it to a managed proxy rather than loading the full row.
	 */
	private @Nullable Team resolveTeam(@Nullable Long teamId) {
		return teamId == null ? null : entityManager.getReference(Team.class, teamId);
	}
}
