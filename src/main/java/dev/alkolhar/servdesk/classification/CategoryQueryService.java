package dev.alkolhar.servdesk.classification;

import dev.alkolhar.servdesk.common.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class CategoryQueryService {

	private final CategoryRepository categoryRepository;

	public CategoryQueryService(CategoryRepository categoryRepository) {
		this.categoryRepository = categoryRepository;
	}

	public Page<Category> findAll(Pageable pageable) {
		return categoryRepository.findAll(pageable);
	}

	public Category findById(Long id) {
		return categoryRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Category " + id + " not found"));
	}
}
