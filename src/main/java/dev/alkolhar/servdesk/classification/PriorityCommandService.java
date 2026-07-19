package dev.alkolhar.servdesk.classification;

import org.springframework.stereotype.Service;

@Service
public class PriorityCommandService {

	private final PriorityRepository priorityRepository;
	private final PriorityQueryService priorityQueryService;

	public PriorityCommandService(PriorityRepository priorityRepository, PriorityQueryService priorityQueryService) {
		this.priorityRepository = priorityRepository;
		this.priorityQueryService = priorityQueryService;
	}

	public Priority create(PriorityCreateRequest request) {
		Priority priority = new Priority();
		priority.setName(request.name());
		priority.setSortOrder(request.sortOrder());
		return priorityRepository.save(priority);
	}

	public Priority update(Long id, PriorityUpdateRequest request) {
		Priority existing = priorityQueryService.findById(id);
		existing.setName(request.name());
		existing.setSortOrder(request.sortOrder());
		return priorityRepository.save(existing);
	}

	public void delete(Long id) {
		priorityRepository.delete(priorityQueryService.findById(id));
	}
}
