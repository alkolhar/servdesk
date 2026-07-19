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
class ImpactCommandServiceTest {

	@Mock
	private ImpactRepository impactRepository;

	@Mock
	private ImpactQueryService impactQueryService;

	private ImpactCommandService commandService;

	@BeforeEach
	void setUp() {
		commandService = new ImpactCommandService(impactRepository, impactQueryService);
	}

	@Test
	void createSetsNameAndSortOrder() {
		when(impactRepository.save(any(Impact.class))).thenAnswer(invocation -> {
			Impact impact = invocation.getArgument(0);
			ReflectionTestUtils.setField(impact, "id", 1L);
			return impact;
		});

		Impact saved = commandService.create(new ImpactCreateRequest("Critical", 0));

		assertThat(saved.getName()).isEqualTo("Critical");
		assertThat(saved.getSortOrder()).isEqualTo(0);
	}

	@Test
	void updateReplacesNameAndSortOrder() {
		Impact existing = new Impact();
		when(impactQueryService.findById(1L)).thenReturn(existing);
		when(impactRepository.save(existing)).thenReturn(existing);

		Impact updated = commandService.update(1L, new ImpactUpdateRequest("Low", 3));

		assertThat(updated.getName()).isEqualTo("Low");
		assertThat(updated.getSortOrder()).isEqualTo(3);
	}

	@Test
	void deleteDelegatesToRepositoryWithTheEntityLoadedByQueryService() {
		Impact existing = new Impact();
		when(impactQueryService.findById(9L)).thenReturn(existing);

		commandService.delete(9L);

		verify(impactRepository).delete(existing);
	}
}
