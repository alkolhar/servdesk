package dev.alkolhar.servdesk.classification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PriorityDefinitionCommandServiceTest {

	@Mock
	private PriorityDefinitionRepository priorityDefinitionRepository;

	@Mock
	private PriorityDefinitionQueryService priorityDefinitionQueryService;

	@Mock
	private EntityManager entityManager;

	private PriorityDefinitionCommandService commandService;

	@BeforeEach
	void setUp() {
		commandService = new PriorityDefinitionCommandService(priorityDefinitionRepository,
				priorityDefinitionQueryService, entityManager);
	}

	@Test
	void createResolvesImpactUrgencyAndPriorityReferences() {
		Impact impact = mock(Impact.class);
		Urgency urgency = mock(Urgency.class);
		Priority priority = mock(Priority.class);
		when(entityManager.getReference(Impact.class, 10L)).thenReturn(impact);
		when(entityManager.getReference(Urgency.class, 20L)).thenReturn(urgency);
		when(entityManager.getReference(Priority.class, 30L)).thenReturn(priority);
		when(priorityDefinitionRepository.save(any(PriorityDefinition.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		PriorityDefinition saved = commandService.create(new PriorityDefinitionCreateRequest(10L, 20L, 30L));

		assertThat(saved.getImpact()).isEqualTo(impact);
		assertThat(saved.getUrgency()).isEqualTo(urgency);
		assertThat(saved.getPriority()).isEqualTo(priority);
	}

	@Test
	void updateReplacesImpactUrgencyAndPriorityReferences() {
		PriorityDefinition existing = new PriorityDefinition();
		when(priorityDefinitionQueryService.findById(1L)).thenReturn(existing);
		Impact impact = mock(Impact.class);
		Urgency urgency = mock(Urgency.class);
		Priority priority = mock(Priority.class);
		when(entityManager.getReference(Impact.class, 11L)).thenReturn(impact);
		when(entityManager.getReference(Urgency.class, 21L)).thenReturn(urgency);
		when(entityManager.getReference(Priority.class, 31L)).thenReturn(priority);
		when(priorityDefinitionRepository.save(existing)).thenReturn(existing);

		PriorityDefinition updated = commandService.update(1L, new PriorityDefinitionUpdateRequest(11L, 21L, 31L));

		assertThat(updated.getImpact()).isEqualTo(impact);
		assertThat(updated.getUrgency()).isEqualTo(urgency);
		assertThat(updated.getPriority()).isEqualTo(priority);
	}

	@Test
	void deleteDelegatesToRepositoryWithTheEntityLoadedByQueryService() {
		PriorityDefinition existing = new PriorityDefinition();
		ReflectionTestUtils.setField(existing, "id", 9L);
		when(priorityDefinitionQueryService.findById(9L)).thenReturn(existing);

		commandService.delete(9L);

		verify(priorityDefinitionRepository).delete(existing);
	}
}
