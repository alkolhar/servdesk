package dev.alkolhar.servdesk.classification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PriorityCommandServiceTest {

	@Mock
	private PriorityRepository priorityRepository;

	@Mock
	private PriorityQueryService priorityQueryService;

	private PriorityCommandService commandService;

	@BeforeEach
	void setUp() {
		commandService = new PriorityCommandService(priorityRepository, priorityQueryService);
	}

	@Test
	void createSetsNameAndSortOrder() {
		when(priorityRepository.save(any(Priority.class))).thenAnswer(invocation -> {
			Priority priority = invocation.getArgument(0);
			ReflectionTestUtils.setField(priority, "id", 1L);
			return priority;
		});

		Priority saved = commandService.create(new PriorityCreateRequest("Critical", 0));

		assertThat(saved.getName()).isEqualTo("Critical");
		assertThat(saved.getSortOrder()).isEqualTo(0);
	}

	@Test
	void updateReplacesNameAndSortOrder() {
		Priority existing = new Priority();
		when(priorityQueryService.findById(1L)).thenReturn(existing);
		when(priorityRepository.save(existing)).thenReturn(existing);

		Priority updated = commandService.update(1L, new PriorityUpdateRequest("Low", 3));

		assertThat(updated.getName()).isEqualTo("Low");
		assertThat(updated.getSortOrder()).isEqualTo(3);
	}

	@Test
	void deleteDelegatesToRepositoryWithTheEntityLoadedByQueryService() {
		Priority existing = new Priority();
		when(priorityQueryService.findById(9L)).thenReturn(existing);

		commandService.delete(9L);

		verify(priorityRepository).delete(existing);
	}
}
