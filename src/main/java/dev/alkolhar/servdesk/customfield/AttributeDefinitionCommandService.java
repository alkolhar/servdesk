package dev.alkolhar.servdesk.customfield;

import dev.alkolhar.servdesk.common.exception.ConflictException;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

@Service
public class AttributeDefinitionCommandService {

	private final AttributeDefinitionRepository definitionRepository;
	private final AttributeDefinitionQueryService queryService;

	public AttributeDefinitionCommandService(AttributeDefinitionRepository definitionRepository,
			AttributeDefinitionQueryService queryService) {
		this.definitionRepository = definitionRepository;
		this.queryService = queryService;
	}

	public AttributeDefinition create(AttributeDefinitionCreateRequest request) {
		requireCoherentEnumValues(request.type(), request.enumValues());
		if (definitionRepository.existsByTargetAndKey(request.target(), request.key())) {
			throw new ConflictException(
					"An attribute definition with key '" + request.key() + "' already exists for " + request.target());
		}
		AttributeDefinition definition = new AttributeDefinition();
		definition.setTarget(request.target());
		definition.setKey(request.key());
		definition.setLabel(request.label());
		definition.setType(request.type());
		definition.setRequired(request.required());
		definition.setEnumValues(request.enumValues());
		return definitionRepository.save(definition);
	}

	/**
	 * {@code target}/{@code key}/{@code type} are immutable — the update request
	 * can't even express them (see {@link AttributeDefinitionUpdateRequest}); only
	 * label, required, and (for ENUM) the value list may change.
	 */
	public AttributeDefinition update(Long id, AttributeDefinitionUpdateRequest request) {
		AttributeDefinition existing = queryService.findById(id);
		requireCoherentEnumValues(existing.getType(), request.enumValues());
		existing.setLabel(request.label());
		existing.setRequired(request.required());
		existing.setEnumValues(request.enumValues());
		return definitionRepository.save(existing);
	}

	public void delete(Long id) {
		definitionRepository.delete(queryService.findById(id));
	}

	private void requireCoherentEnumValues(AttributeType type, @Nullable List<String> enumValues) {
		if (type == AttributeType.ENUM && (enumValues == null || enumValues.isEmpty())) {
			throw new IllegalArgumentException("An ENUM attribute definition needs a non-empty enumValues list");
		}
		if (type != AttributeType.ENUM && enumValues != null) {
			throw new IllegalArgumentException("enumValues is only allowed for ENUM attribute definitions");
		}
	}
}
