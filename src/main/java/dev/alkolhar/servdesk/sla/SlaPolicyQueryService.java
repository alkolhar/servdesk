package dev.alkolhar.servdesk.sla;

import dev.alkolhar.servdesk.common.exception.NotFoundException;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class SlaPolicyQueryService {

	private final SlaPolicyRepository policyRepository;

	public SlaPolicyQueryService(SlaPolicyRepository policyRepository) {
		this.policyRepository = policyRepository;
	}

	/**
	 * Deliberately not paginated: at most one policy per priority, and priorities
	 * are a handful of rows of admin configuration.
	 */
	public List<SlaPolicy> findAll() {
		return policyRepository.findAll();
	}

	public SlaPolicy findById(Long id) {
		return policyRepository.findById(id).orElseThrow(() -> new NotFoundException("SlaPolicy " + id + " not found"));
	}
}
