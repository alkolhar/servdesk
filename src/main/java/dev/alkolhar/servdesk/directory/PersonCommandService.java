package dev.alkolhar.servdesk.directory;

import dev.alkolhar.servdesk.common.exception.ConflictException;
import dev.alkolhar.servdesk.directory.event.PersonCreatedEvent;
import jakarta.persistence.EntityManager;
import org.jspecify.annotations.Nullable;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

	/**
	 * Every field replaces what the row holds, as PUT implies — except the three
	 * that carry a login. A null {@code username}, {@code password} or
	 * {@code enabled} leaves the stored value alone, so a client that updates a
	 * name or a team without echoing back credentials cannot revoke access by
	 * omission (issue #66). The cost of that choice is that none of the three can
	 * be *cleared* through this endpoint; revoking a login needs an operation that
	 * says so.
	 *
	 * @see PersonUpdateRequest
	 */
	@Transactional
	public Person update(Long id, PersonUpdateRequest request) {
		Person existing = personQueryService.findById(id);
		if (isLoginCapableAgent(existing) && !wouldRemainLoginCapable(existing, request)) {
			requireAnotherLoginCapableAgent(existing, "demote or disable");
		}
		existing.setRole(request.role());
		existing.setName(request.name());
		existing.setEmail(request.email());
		existing.setPhone(request.phone());
		if (request.username() != null) {
			existing.setUsername(request.username());
		}
		if (request.password() != null) {
			existing.setPassword(passwordEncoder.encode(request.password()));
		}
		if (request.enabled() != null) {
			existing.setEnabled(request.enabled());
		}
		existing.setTeam(resolveTeam(request.teamId()));
		return personRepository.save(existing);
	}

	@Transactional
	public void delete(Long id) {
		Person existing = personQueryService.findById(id);
		if (isLoginCapableAgent(existing)) {
			requireAnotherLoginCapableAgent(existing, "delete");
		}
		personRepository.delete(existing);
	}

	/**
	 * The last-Agent invariant (issue #63): a deployment must never be left without
	 * someone who can log in and administer it. {@code /api/setup} is no escape
	 * hatch — it only runs while the database holds no {@link Person} at all, so a
	 * deployment that loses its last credentialed Agent is bricked outright rather
	 * than recoverable.
	 * <p>
	 * 409 rather than 403: the caller is a fully authorised Agent and the answer
	 * does not change if a different Agent asks. Nothing about the caller is wrong,
	 * so a 403 would falsely imply that more privilege would help. What is wrong is
	 * the state the request would leave the directory in.
	 * <p>
	 * The check and the write share a transaction, but two callers removing the
	 * last two Agents at the same instant can still both pass it — an accepted
	 * residual, on the same footing as the live-duplicate case the service layer
	 * also cannot see coming. Postgres cannot express "at least one row matching a
	 * predicate" as a constraint, so closing it would take SERIALIZABLE or an
	 * advisory lock: disproportionate for an administrative action two people would
	 * have to race on the same second.
	 * <p>
	 * Deliberately not cached. The query only runs on the rare write that would
	 * actually cost someone their login — never on an ordinary edit — and a cache
	 * would go stale in precisely the situation the invariant exists for: a stale
	 * "someone else can log in" is how the deployment gets bricked.
	 */
	private void requireAnotherLoginCapableAgent(Person person, String action) {
		if (!personQueryService.anotherLoginCapableAgentExists(person.getId())) {
			throw new ConflictException("Cannot " + action
					+ " the last agent who can log in — the deployment would be left unadministrable");
		}
	}

	/**
	 * Login-capable means the person can actually authenticate and administer:
	 * {@link PersonUserDetailsService} resolves a login through
	 * {@code findByUsername}, and Spring Security rejects a disabled account. An
	 * Agent missing any of the three is a directory entry, not an administrator.
	 */
	private static boolean isLoginCapableAgent(Person person) {
		return person.getRole() == PersonRole.AGENT && person.getUsername() != null && person.getPassword() != null
				&& person.isEnabled();
	}

	/**
	 * What the person would be once {@code request} is applied — evaluated before
	 * anything is mutated, so the rejection never depends on a rollback to undo a
	 * half-applied change. Null {@code username}/{@code password}/{@code enabled}
	 * leave the stored values in place (issue #66), which is why each term falls
	 * back to what the row already holds.
	 */
	private static boolean wouldRemainLoginCapable(Person existing, PersonUpdateRequest request) {
		boolean enabled = request.enabled() == null ? existing.isEnabled() : request.enabled();
		String username = request.username() == null ? existing.getUsername() : request.username();
		String password = request.password() == null ? existing.getPassword() : request.password();
		return request.role() == PersonRole.AGENT && username != null && password != null && enabled;
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
