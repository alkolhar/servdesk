package dev.alkolhar.servdesk.classification;

import dev.alkolhar.servdesk.common.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class PriorityQueryService {

	private final PriorityRepository priorityRepository;

	public PriorityQueryService(PriorityRepository priorityRepository) {
		this.priorityRepository = priorityRepository;
	}

	public Page<Priority> findAll(Pageable pageable) {
		return priorityRepository.findAll(pageable);
	}

	public Priority findById(Long id) {
		return priorityRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("Priority " + id + " not found"));
	}
}
