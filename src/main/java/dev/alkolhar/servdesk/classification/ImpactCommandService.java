package dev.alkolhar.servdesk.classification;

import org.springframework.stereotype.Service;

@Service
public class ImpactCommandService {

	private final ImpactRepository impactRepository;
	private final ImpactQueryService impactQueryService;

	public ImpactCommandService(ImpactRepository impactRepository, ImpactQueryService impactQueryService) {
		this.impactRepository = impactRepository;
		this.impactQueryService = impactQueryService;
	}

	public Impact create(ImpactCreateRequest request) {
		Impact impact = new Impact();
		impact.setName(request.name());
		impact.setSortOrder(request.sortOrder());
		return impactRepository.save(impact);
	}

	public Impact update(Long id, ImpactUpdateRequest request) {
		Impact existing = impactQueryService.findById(id);
		existing.setName(request.name());
		existing.setSortOrder(request.sortOrder());
		return impactRepository.save(existing);
	}

	public void delete(Long id) {
		impactRepository.delete(impactQueryService.findById(id));
	}
}
