package dev.alkolhar.servdesk.sla;

import dev.alkolhar.servdesk.classification.Priority;
import dev.alkolhar.servdesk.common.exception.ConflictException;
import jakarta.persistence.EntityManager;
import org.springframework.stereotype.Service;

/**
 * Policy changes deliberately do <b>not</b> recompute deadlines on existing
 * tickets — a ticket's {@code respondBy}/{@code resolveBy} reflect the policy
 * at the time it was created (or last re-prioritized), the same
 * written-under-the-rules-of-its-time semantics the custom-field mechanism
 * uses. Documented on issue #31.
 */
@Service
public class SlaPolicyCommandService {

	private final SlaPolicyRepository policyRepository;
	private final SlaPolicyQueryService queryService;
	private final EntityManager entityManager;

	public SlaPolicyCommandService(SlaPolicyRepository policyRepository, SlaPolicyQueryService queryService,
			EntityManager entityManager) {
		this.policyRepository = policyRepository;
		this.queryService = queryService;
		this.entityManager = entityManager;
	}

	public SlaPolicy create(SlaPolicyCreateRequest request) {
		requireATarget(request.responseMinutes(), request.resolutionMinutes());
		if (policyRepository.existsByPriorityId(request.priorityId())) {
			throw new ConflictException("An SLA policy for priority " + request.priorityId() + " already exists");
		}
		SlaPolicy policy = new SlaPolicy();
		policy.setPriority(entityManager.getReference(Priority.class, request.priorityId()));
		policy.setResponseMinutes(request.responseMinutes());
		policy.setResolutionMinutes(request.resolutionMinutes());
		return policyRepository.save(policy);
	}

	public SlaPolicy update(Long id, SlaPolicyUpdateRequest request) {
		requireATarget(request.responseMinutes(), request.resolutionMinutes());
		SlaPolicy existing = queryService.findById(id);
		existing.setResponseMinutes(request.responseMinutes());
		existing.setResolutionMinutes(request.resolutionMinutes());
		return policyRepository.save(existing);
	}

	public void delete(Long id) {
		policyRepository.delete(queryService.findById(id));
	}

	private void requireATarget(Integer responseMinutes, Integer resolutionMinutes) {
		if (responseMinutes == null && resolutionMinutes == null) {
			throw new IllegalArgumentException("An SLA policy needs at least one of responseMinutes/resolutionMinutes");
		}
	}
}
