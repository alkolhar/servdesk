package dev.alkolhar.servdesk.classification;

import dev.alkolhar.servdesk.common.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ImpactQueryService {

	private final ImpactRepository impactRepository;

	public ImpactQueryService(ImpactRepository impactRepository) {
		this.impactRepository = impactRepository;
	}

	public Page<Impact> findAll(Pageable pageable) {
		return impactRepository.findAll(pageable);
	}

	public Impact findById(Long id) {
		return impactRepository.findById(id).orElseThrow(() -> new NotFoundException("Impact " + id + " not found"));
	}
}
