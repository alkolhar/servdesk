package dev.alkolhar.servdesk.classification;

import org.springframework.stereotype.Service;

@Service
public class UrgencyCommandService {

	private final UrgencyRepository urgencyRepository;
	private final UrgencyQueryService urgencyQueryService;

	public UrgencyCommandService(UrgencyRepository urgencyRepository, UrgencyQueryService urgencyQueryService) {
		this.urgencyRepository = urgencyRepository;
		this.urgencyQueryService = urgencyQueryService;
	}

	public Urgency create(UrgencyCreateRequest request) {
		Urgency urgency = new Urgency();
		urgency.setName(request.name());
		urgency.setSortOrder(request.sortOrder());
		return urgencyRepository.save(urgency);
	}

	public Urgency update(Long id, UrgencyUpdateRequest request) {
		Urgency existing = urgencyQueryService.findById(id);
		existing.setName(request.name());
		existing.setSortOrder(request.sortOrder());
		return urgencyRepository.save(existing);
	}

	public void delete(Long id) {
		urgencyRepository.delete(urgencyQueryService.findById(id));
	}
}
