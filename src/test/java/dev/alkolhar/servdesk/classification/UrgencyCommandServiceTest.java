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
class UrgencyCommandServiceTest {

	@Mock
	private UrgencyRepository urgencyRepository;

	@Mock
	private UrgencyQueryService urgencyQueryService;

	private UrgencyCommandService commandService;

	@BeforeEach
	void setUp() {
		commandService = new UrgencyCommandService(urgencyRepository, urgencyQueryService);
	}

	@Test
	void createSetsNameAndSortOrder() {
		when(urgencyRepository.save(any(Urgency.class))).thenAnswer(invocation -> {
			Urgency urgency = invocation.getArgument(0);
			ReflectionTestUtils.setField(urgency, "id", 1L);
			return urgency;
		});

		Urgency saved = commandService.create(new UrgencyCreateRequest("Critical", 0));

		assertThat(saved.getName()).isEqualTo("Critical");
		assertThat(saved.getSortOrder()).isEqualTo(0);
	}

	@Test
	void updateReplacesNameAndSortOrder() {
		Urgency existing = new Urgency();
		when(urgencyQueryService.findById(1L)).thenReturn(existing);
		when(urgencyRepository.save(existing)).thenReturn(existing);

		Urgency updated = commandService.update(1L, new UrgencyUpdateRequest("Low", 3));

		assertThat(updated.getName()).isEqualTo("Low");
		assertThat(updated.getSortOrder()).isEqualTo(3);
	}

	@Test
	void deleteDelegatesToRepositoryWithTheEntityLoadedByQueryService() {
		Urgency existing = new Urgency();
		when(urgencyQueryService.findById(9L)).thenReturn(existing);

		commandService.delete(9L);

		verify(urgencyRepository).delete(existing);
	}
}
