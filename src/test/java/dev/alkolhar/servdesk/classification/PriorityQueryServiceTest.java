package dev.alkolhar.servdesk.classification;

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
class PriorityQueryServiceTest {

	@Mock
	private PriorityRepository priorityRepository;

	private PriorityQueryService queryService;

	@BeforeEach
	void setUp() {
		queryService = new PriorityQueryService(priorityRepository);
	}

	@Test
	void findAllDelegatesToRepository() {
		Pageable pageable = Pageable.ofSize(20);
		Page<Priority> page = new PageImpl<>(List.of());
		when(priorityRepository.findAll(pageable)).thenReturn(page);

		assertThat(queryService.findAll(pageable)).isSameAs(page);
	}

	@Test
	void findByIdThrowsNotFoundExceptionWhenMissing() {
		when(priorityRepository.findById(999L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> queryService.findById(999L)).isInstanceOf(NotFoundException.class)
				.hasMessageContaining("999");
	}

	@Test
	void findByIdReturnsThePriorityWhenPresent() {
		Priority priority = new Priority();
		when(priorityRepository.findById(1L)).thenReturn(Optional.of(priority));

		assertThat(queryService.findById(1L)).isSameAs(priority);
	}
}
