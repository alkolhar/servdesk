package dev.alkolhar.servdesk.customfield;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * Validates a full {@code attributes} map against the live definitions for a
 * target — the write-time choke point of the custom-field mechanism (reads
 * never validate, so rows written under older definitions stay readable
 * untouched). Rejections throw {@link IllegalArgumentException}, which
 * {@code RestExceptionHandler} maps to 400 per the convention documented there
 * (unusable input → 400; broken server invariant →
 * {@link IllegalStateException} → 500).
 */
@Service
public class AttributeValidator {

	private final AttributeDefinitionRepository definitionRepository;

	public AttributeValidator(AttributeDefinitionRepository definitionRepository) {
		this.definitionRepository = definitionRepository;
	}

	public void validate(AttributeTarget target, Map<String, Object> attributes) {
		List<AttributeDefinition> definitions = definitionRepository.findByTargetOrderByKeyAsc(target);
		Map<String, AttributeDefinition> byKey = definitions.stream()
				.collect(java.util.stream.Collectors.toMap(AttributeDefinition::getKey, definition -> definition));

		for (Map.Entry<String, Object> entry : attributes.entrySet()) {
			AttributeDefinition definition = byKey.get(entry.getKey());
			if (definition == null) {
				throw new IllegalArgumentException(
						"Unknown attribute '" + entry.getKey() + "' — no definition exists for target " + target);
			}
			validateValue(definition, entry.getValue());
		}
		for (AttributeDefinition definition : definitions) {
			if (definition.isRequired() && attributes.get(definition.getKey()) == null) {
				throw new IllegalArgumentException("Required attribute '" + definition.getKey() + "' is missing");
			}
		}
	}

	private void validateValue(AttributeDefinition definition, Object value) {
		if (value == null) {
			return; // an explicit null clears the value; required-ness is checked separately
		}
		switch (definition.getType()) {
			case STRING -> require(value instanceof String, definition, "a string", value);
			case NUMBER -> require(value instanceof Number, definition, "a number", value);
			case BOOLEAN -> require(value instanceof Boolean, definition, "a boolean", value);
			case DATE -> {
				require(value instanceof String, definition, "an ISO-8601 date string (yyyy-MM-dd)", value);
				try {
					LocalDate.parse((String) value);
				} catch (DateTimeParseException ex) {
					throw mismatch(definition, "an ISO-8601 date string (yyyy-MM-dd)", value);
				}
			}
			case ENUM -> {
				require(value instanceof String, definition, "one of its enum values", value);
				List<String> allowed = definition.getEnumValues();
				if (allowed == null || !allowed.contains(value)) {
					throw new IllegalArgumentException("Attribute '" + definition.getKey() + "' must be one of "
							+ allowed + " but was '" + value + "'");
				}
			}
		}
	}

	private void require(boolean condition, AttributeDefinition definition, String expected, Object value) {
		if (!condition) {
			throw mismatch(definition, expected, value);
		}
	}

	private IllegalArgumentException mismatch(AttributeDefinition definition, String expected, Object value) {
		return new IllegalArgumentException(
				"Attribute '" + definition.getKey() + "' must be " + expected + " but was '" + value + "'");
	}
}
