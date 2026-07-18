package dev.alkolhar.servdesk.directory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import dev.alkolhar.servdesk.common.exception.ConflictException;
import dev.alkolhar.servdesk.directory.event.PersonCreatedEvent;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Pure Mockito unit tests: no Spring context, no database.
 * {@link PersonControllerTest} already covers this service end-to-end through
 * the real HTTP/security/persistence stack — these tests exist for the internal
 * decisions that class can't easily pin down (does an unset password stay
 * unset, exactly what does the published event carry, is a repeat setup attempt
 * rejected before ever touching the repository).
 */
@ExtendWith(MockitoExtension.class)
class PersonCommandServiceTest {

	@Mock
	private PersonRepository personRepository;

	@Mock
	private PersonQueryService personQueryService;

	@Mock
	private EntityManager entityManager;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private ApplicationEventPublisher events;

	private PersonCommandService commandService;

	@BeforeEach
	void setUp() {
		commandService = new PersonCommandService(personRepository, personQueryService, entityManager, passwordEncoder,
				events);
	}

	@Test
	void createEncodesPasswordWhenPresent() {
		stubSaveToAssignId(43L);
		when(passwordEncoder.encode("plaintext")).thenReturn("encoded");

		Person saved = commandService.create(new PersonCreateRequest(PersonRole.AGENT, "Ada Agent", "ada@example.com",
				null, "ada", "plaintext", null));

		assertThat(saved.getPassword()).isEqualTo("encoded");
	}

	@Test
	void createLeavesPasswordNullWhenRequestPasswordIsNull() {
		stubSaveToAssignId(44L);

		Person saved = commandService.create(new PersonCreateRequest(PersonRole.CUSTOMER, "Cara Customer",
				"cara@example.com", null, null, null, null));

		assertThat(saved.getPassword()).isNull();
		verifyNoInteractions(passwordEncoder);
	}

	@Test
	void createResolvesTeamReferenceOnlyWhenTeamIdPresent() {
		stubSaveToAssignId(45L);
		Team teamProxy = new Team();
		when(entityManager.getReference(Team.class, 7L)).thenReturn(teamProxy);

		Person saved = commandService.create(
				new PersonCreateRequest(PersonRole.AGENT, "Ada Agent", "ada@example.com", null, null, null, 7L));

		assertThat(saved.getTeam()).isSameAs(teamProxy);
	}

	@Test
	void createLeavesTeamNullWhenTeamIdAbsent() {
		stubSaveToAssignId(46L);

		Person saved = commandService.create(
				new PersonCreateRequest(PersonRole.AGENT, "Ada Agent", "ada@example.com", null, null, null, null));

		assertThat(saved.getTeam()).isNull();
		verifyNoInteractions(entityManager);
	}

	@Test
	void createPublishesPersonCreatedEventWithSavedIdAndRole() {
		stubSaveToAssignId(47L);

		commandService.create(new PersonCreateRequest(PersonRole.CUSTOMER, "Cara Customer", "cara@example.com", null,
				null, null, null));

		ArgumentCaptor<PersonCreatedEvent> captor = ArgumentCaptor.forClass(PersonCreatedEvent.class);
		verify(events).publishEvent(captor.capture());
		assertThat(captor.getValue().personId()).isEqualTo(47L);
		assertThat(captor.getValue().role()).isEqualTo(PersonRole.CUSTOMER);
	}

	@Test
	void updateKeepsExistingPasswordWhenRequestPasswordIsNull() {
		Person existing = new Person();
		existing.setPassword("already-encoded");
		when(personQueryService.findById(1L)).thenReturn(existing);
		when(personRepository.save(existing)).thenReturn(existing);

		Person updated = commandService.update(1L, new PersonUpdateRequest(PersonRole.AGENT, "Ada Agent",
				"ada@example.com", null, null, null, true, null));

		assertThat(updated.getPassword()).isEqualTo("already-encoded");
		verifyNoInteractions(passwordEncoder);
	}

	@Test
	void updateEncodesPasswordWhenRequestPasswordIsPresent() {
		Person existing = new Person();
		when(personQueryService.findById(1L)).thenReturn(existing);
		when(personRepository.save(existing)).thenReturn(existing);
		when(passwordEncoder.encode("newpass")).thenReturn("newly-encoded");

		Person updated = commandService.update(1L, new PersonUpdateRequest(PersonRole.AGENT, "Ada Agent",
				"ada@example.com", null, null, "newpass", true, null));

		assertThat(updated.getPassword()).isEqualTo("newly-encoded");
	}

	@Test
	void createInitialAgentRejectsWhenSetupAlreadyCompleted() {
		when(personQueryService.isSetupRequired()).thenReturn(false);

		assertThatThrownBy(() -> commandService.createInitialAgent("Admin", "admin@example.com", null, "admin", "pw"))
				.isInstanceOf(ConflictException.class);

		verify(personRepository, never()).save(any());
		verifyNoInteractions(events);
	}

	@Test
	void createInitialAgentSucceedsAndPublishesEventWhenSetupRequired() {
		when(personQueryService.isSetupRequired()).thenReturn(true);
		stubSaveToAssignId(1L);
		when(passwordEncoder.encode("pw")).thenReturn("encoded-pw");

		Person saved = commandService.createInitialAgent("Admin", "admin@example.com", null, "admin", "pw");

		assertThat(saved.getRole()).isEqualTo(PersonRole.AGENT);
		assertThat(saved.getPassword()).isEqualTo("encoded-pw");
		verify(events).publishEvent(any(PersonCreatedEvent.class));
	}

	@Test
	void deleteDelegatesToRepositoryWithTheEntityLoadedByQueryService() {
		Person existing = new Person();
		when(personQueryService.findById(9L)).thenReturn(existing);

		commandService.delete(9L);

		verify(personRepository).delete(existing);
	}

	private void stubSaveToAssignId(long id) {
		when(personRepository.save(any(Person.class))).thenAnswer(invocation -> {
			Person person = invocation.getArgument(0);
			ReflectionTestUtils.setField(person, "id", id);
			return person;
		});
	}
}
