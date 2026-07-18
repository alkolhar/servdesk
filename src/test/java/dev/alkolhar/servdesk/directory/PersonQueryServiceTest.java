package dev.alkolhar.servdesk.directory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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
class PersonQueryServiceTest {

	@Mock
	private PersonRepository personRepository;

	private PersonQueryService queryService;

	@BeforeEach
	void setUp() {
		queryService = new PersonQueryService(personRepository);
	}

	@Test
	void findAllWithoutRoleFilterDelegatesToPlainFindAll() {
		Pageable pageable = Pageable.ofSize(20);
		Page<Person> page = new PageImpl<>(List.of());
		when(personRepository.findAll(pageable)).thenReturn(page);

		Page<Person> result = queryService.findAll(null, pageable);

		assertThat(result).isSameAs(page);
		verify(personRepository, never()).findByRole(any(), any());
	}

	@Test
	void findAllWithRoleFilterDelegatesToFindByRole() {
		Pageable pageable = Pageable.ofSize(20);
		Page<Person> page = new PageImpl<>(List.of());
		when(personRepository.findByRole(PersonRole.AGENT, pageable)).thenReturn(page);

		Page<Person> result = queryService.findAll(PersonRole.AGENT, pageable);

		assertThat(result).isSameAs(page);
		verify(personRepository, never()).findAll(pageable);
	}

	@Test
	void findByIdThrowsNotFoundExceptionWhenMissing() {
		when(personRepository.findById(999L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> queryService.findById(999L)).isInstanceOf(NotFoundException.class)
				.hasMessageContaining("999");
	}

	@Test
	void findByIdReturnsThePersonWhenPresent() {
		Person person = new Person();
		when(personRepository.findById(1L)).thenReturn(Optional.of(person));

		assertThat(queryService.findById(1L)).isSameAs(person);
	}

	@Test
	void isSetupRequiredReflectsWhetherAnyPersonExists() {
		when(personRepository.count()).thenReturn(0L);
		assertThat(queryService.isSetupRequired()).isTrue();

		when(personRepository.count()).thenReturn(1L);
		assertThat(queryService.isSetupRequired()).isFalse();
	}
}
