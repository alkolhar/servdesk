package dev.alkolhar.servdesk.classification;

import dev.alkolhar.servdesk.common.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class PriorityDefinitionQueryService {

	private final PriorityDefinitionRepository priorityDefinitionRepository;

	public PriorityDefinitionQueryService(PriorityDefinitionRepository priorityDefinitionRepository) {
		this.priorityDefinitionRepository = priorityDefinitionRepository;
	}

	public Page<PriorityDefinition> findAll(Pageable pageable) {
		return priorityDefinitionRepository.findAll(pageable);
	}

	public PriorityDefinition findById(Long id) {
		return priorityDefinitionRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("PriorityDefinition " + id + " not found"));
	}
}
