package dev.alkolhar.servdesk.classification;

import jakarta.persistence.EntityManager;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

@Service
public class CategoryCommandService {

	private final CategoryRepository categoryRepository;
	private final CategoryQueryService categoryQueryService;
	private final EntityManager entityManager;

	public CategoryCommandService(CategoryRepository categoryRepository, CategoryQueryService categoryQueryService,
			EntityManager entityManager) {
		this.categoryRepository = categoryRepository;
		this.categoryQueryService = categoryQueryService;
		this.entityManager = entityManager;
	}

	public Category create(CategoryCreateRequest request) {
		Category category = new Category();
		category.setName(request.name());
		category.setParent(resolveParent(request.parentId()));
		return categoryRepository.save(category);
	}

	public Category update(Long id, CategoryUpdateRequest request) {
		Category existing = categoryQueryService.findById(id);
		existing.setName(request.name());
		existing.setParent(resolveParent(request.parentId()));
		return categoryRepository.save(existing);
	}

	public void delete(Long id) {
		categoryRepository.delete(categoryQueryService.findById(id));
	}

	/**
	 * The request carries {@code parentId} as a plain id rather than a nested
	 * object; resolve it to a managed proxy rather than loading the full row.
	 */
	private @Nullable Category resolveParent(@Nullable Long parentId) {
		return parentId == null ? null : entityManager.getReference(Category.class, parentId);
	}
}
