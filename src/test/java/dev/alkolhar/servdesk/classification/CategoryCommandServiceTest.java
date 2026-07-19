package dev.alkolhar.servdesk.classification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Pure Mockito unit tests, mirroring {@code PersonCommandServiceTest}:
 * {@link CategoryControllerTest} already covers this service end-to-end through
 * the real HTTP/security/persistence stack — these exist for the internal
 * decision that class can't easily pin down: is {@code parentId} resolved to a
 * managed reference only when present.
 */
@ExtendWith(MockitoExtension.class)
class CategoryCommandServiceTest {

	@Mock
	private CategoryRepository categoryRepository;

	@Mock
	private CategoryQueryService categoryQueryService;

	@Mock
	private EntityManager entityManager;

	private CategoryCommandService commandService;

	@BeforeEach
	void setUp() {
		commandService = new CategoryCommandService(categoryRepository, categoryQueryService, entityManager);
	}

	@Test
	void createResolvesParentReferenceOnlyWhenParentIdPresent() {
		stubSaveToAssignId(1L);
		Category parentProxy = new Category();
		when(entityManager.getReference(Category.class, 7L)).thenReturn(parentProxy);

		Category saved = commandService.create(new CategoryCreateRequest("Laptop", 7L));

		assertThat(saved.getParent()).isSameAs(parentProxy);
	}

	@Test
	void createLeavesParentNullWhenParentIdAbsent() {
		stubSaveToAssignId(2L);

		Category saved = commandService.create(new CategoryCreateRequest("Hardware", null));

		assertThat(saved.getParent()).isNull();
		verifyNoInteractions(entityManager);
	}

	@Test
	void updateReplacesParentReference() {
		Category existing = new Category();
		when(categoryQueryService.findById(1L)).thenReturn(existing);
		when(categoryRepository.save(existing)).thenReturn(existing);
		Category newParentProxy = new Category();
		when(entityManager.getReference(Category.class, 9L)).thenReturn(newParentProxy);

		Category updated = commandService.update(1L, new CategoryUpdateRequest("Battery", 9L));

		assertThat(updated.getName()).isEqualTo("Battery");
		assertThat(updated.getParent()).isSameAs(newParentProxy);
	}

	@Test
	void deleteDelegatesToRepositoryWithTheEntityLoadedByQueryService() {
		Category existing = new Category();
		when(categoryQueryService.findById(5L)).thenReturn(existing);

		commandService.delete(5L);

		verify(categoryRepository).delete(existing);
	}

	private void stubSaveToAssignId(long id) {
		when(categoryRepository.save(any(Category.class))).thenAnswer(invocation -> {
			Category category = invocation.getArgument(0);
			ReflectionTestUtils.setField(category, "id", id);
			return category;
		});
	}
}
