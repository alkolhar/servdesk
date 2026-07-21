package dev.alkolhar.servdesk.customfield;

import dev.alkolhar.servdesk.common.exception.NotFoundException;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

@Service
public class AttributeDefinitionQueryService {

	private final AttributeDefinitionRepository definitionRepository;

	public AttributeDefinitionQueryService(AttributeDefinitionRepository definitionRepository) {
		this.definitionRepository = definitionRepository;
	}

	/**
	 * Deliberately not paginated: the definition set is per-deployment admin
	 * configuration (tens, not thousands), and consumers (admin UI, form rendering)
	 * always want the whole set for a target.
	 */
	public List<AttributeDefinition> findAll(@Nullable AttributeTarget target) {
		return target == null ? definitionRepository.findAll() : definitionRepository.findByTargetOrderByKeyAsc(target);
	}

	public AttributeDefinition findById(Long id) {
		return definitionRepository.findById(id)
				.orElseThrow(() -> new NotFoundException("AttributeDefinition " + id + " not found"));
	}
}
