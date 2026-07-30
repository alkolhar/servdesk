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
class PriorityDefinitionQueryServiceTest {

	@Mock
	private PriorityDefinitionRepository priorityDefinitionRepository;

	private PriorityDefinitionQueryService queryService;

	@BeforeEach
	void setUp() {
		queryService = new PriorityDefinitionQueryService(priorityDefinitionRepository);
	}

	@Test
	void findAllDelegatesToRepository() {
		Pageable pageable = Pageable.ofSize(20);
		Page<PriorityDefinition> page = new PageImpl<>(List.of());
		when(priorityDefinitionRepository.findAll(pageable)).thenReturn(page);

		assertThat(queryService.findAll(pageable)).isSameAs(page);
	}

	@Test
	void findByIdThrowsNotFoundExceptionWhenMissing() {
		when(priorityDefinitionRepository.findById(999L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> queryService.findById(999L)).isInstanceOf(NotFoundException.class)
				.hasMessageContaining("999");
	}

	@Test
	void findByIdReturnsThePriorityDefinitionWhenPresent() {
		PriorityDefinition definition = new PriorityDefinition();
		when(priorityDefinitionRepository.findById(1L)).thenReturn(Optional.of(definition));

		assertThat(queryService.findById(1L)).isSameAs(definition);
	}
}
