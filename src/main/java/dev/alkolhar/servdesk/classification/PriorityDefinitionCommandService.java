package dev.alkolhar.servdesk.classification;

import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;

@Service
public class PriorityDefinitionCommandService {

	private final PriorityDefinitionRepository priorityDefinitionRepository;
	private final PriorityDefinitionQueryService priorityDefinitionQueryService;
	private final EntityManager entityManager;

	public PriorityDefinitionCommandService(PriorityDefinitionRepository priorityDefinitionRepository,
			PriorityDefinitionQueryService priorityDefinitionQueryService, EntityManager entityManager) {
		this.priorityDefinitionRepository = priorityDefinitionRepository;
		this.priorityDefinitionQueryService = priorityDefinitionQueryService;
		this.entityManager = entityManager;
	}

	public PriorityDefinition create(PriorityDefinitionCreateRequest request) {
		PriorityDefinition definition = new PriorityDefinition();
		definition.setImpact(entityManager.getReference(Impact.class, request.impactId()));
		definition.setUrgency(entityManager.getReference(Urgency.class, request.urgencyId()));
		definition.setPriority(entityManager.getReference(Priority.class, request.priorityId()));
		return priorityDefinitionRepository.save(definition);
	}

	public PriorityDefinition update(Long id, PriorityDefinitionUpdateRequest request) {
		PriorityDefinition existing = priorityDefinitionQueryService.findById(id);
		existing.setImpact(entityManager.getReference(Impact.class, request.impactId()));
		existing.setUrgency(entityManager.getReference(Urgency.class, request.urgencyId()));
		existing.setPriority(entityManager.getReference(Priority.class, request.priorityId()));
		return priorityDefinitionRepository.save(existing);
	}

	public void delete(Long id) {
		priorityDefinitionRepository.delete(priorityDefinitionQueryService.findById(id));
	}
}
