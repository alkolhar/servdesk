package dev.alkolhar.servdesk.classification;

import dev.alkolhar.servdesk.common.exception.NotFoundException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class UrgencyQueryService {

	private final UrgencyRepository urgencyRepository;

	public UrgencyQueryService(UrgencyRepository urgencyRepository) {
		this.urgencyRepository = urgencyRepository;
	}

	public Page<Urgency> findAll(Pageable pageable) {
		return urgencyRepository.findAll(pageable);
	}

	public Urgency findById(Long id) {
		return urgencyRepository.findById(id).orElseThrow(() -> new NotFoundException("Urgency " + id + " not found"));
	}
}
