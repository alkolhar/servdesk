package dev.alkolhar.servdesk.customfield;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

/**
 * Pins down the write-time validation rules of the custom-field mechanism
 * (issue #29) against a mocked definition set — the integration level
 * ({@code TicketAttributesTest}) only proves the 400 surfaces, not each rule.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AttributeValidatorTest {

	@Mock
	private AttributeDefinitionRepository definitionRepository;

	private AttributeValidator validator;

	@BeforeEach
	void setUp() {
		validator = new AttributeValidator(definitionRepository);
		when(definitionRepository.findByTargetOrderByKeyAsc(AttributeTarget.TICKET))
				.thenReturn(List.of(definition("po_number", AttributeType.STRING, false, null),
						definition("impacted_users", AttributeType.NUMBER, false, null),
						definition("vip", AttributeType.BOOLEAN, false, null),
						definition("due_date", AttributeType.DATE, false, null),
						definition("environment", AttributeType.ENUM, false, List.of("prod", "test"))));
	}

	@Test
	void acceptsAValidMapOfEveryType() {
		assertThatCode(() -> validator.validate(AttributeTarget.TICKET, Map.of("po_number", "PO-4711", "impacted_users",
				250, "vip", true, "due_date", "2026-08-01", "environment", "prod"))).doesNotThrowAnyException();
	}

	@Test
	void acceptsAnEmptyMapWhenNothingIsRequired() {
		assertThatCode(() -> validator.validate(AttributeTarget.TICKET, Map.of())).doesNotThrowAnyException();
	}

	@Test
	void rejectsAnUnknownKey() {
		assertThatThrownBy(() -> validator.validate(AttributeTarget.TICKET, Map.of("no_such_key", "x")))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("no_such_key");
	}

	@Test
	void rejectsTypeMismatches() {
		assertThatThrownBy(() -> validator.validate(AttributeTarget.TICKET, Map.of("impacted_users", "many")))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("impacted_users");
		assertThatThrownBy(() -> validator.validate(AttributeTarget.TICKET, Map.of("vip", "yes")))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("vip");
		assertThatThrownBy(() -> validator.validate(AttributeTarget.TICKET, Map.of("po_number", 42)))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("po_number");
	}

	@Test
	void rejectsANonIsoDate() {
		assertThatThrownBy(() -> validator.validate(AttributeTarget.TICKET, Map.of("due_date", "01.08.2026")))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("due_date");
	}

	@Test
	void rejectsAValueOutsideTheEnumList() {
		assertThatThrownBy(() -> validator.validate(AttributeTarget.TICKET, Map.of("environment", "staging")))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("environment");
	}

	@Test
	void rejectsAMissingRequiredAttributeAndAcceptsItWhenPresent() {
		when(definitionRepository.findByTargetOrderByKeyAsc(AttributeTarget.TICKET))
				.thenReturn(List.of(definition("environment", AttributeType.ENUM, true, List.of("prod", "test"))));

		assertThatThrownBy(() -> validator.validate(AttributeTarget.TICKET, Map.of()))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("environment");
		assertThatCode(() -> validator.validate(AttributeTarget.TICKET, Map.of("environment", "test")))
				.doesNotThrowAnyException();
	}

	@Test
	void treatsAnExplicitNullAsMissingForARequiredAttribute() {
		when(definitionRepository.findByTargetOrderByKeyAsc(AttributeTarget.TICKET))
				.thenReturn(List.of(definition("po_number", AttributeType.STRING, true, null)));

		Map<String, Object> attributes = new HashMap<>();
		attributes.put("po_number", null);
		assertThatThrownBy(() -> validator.validate(AttributeTarget.TICKET, attributes))
				.isInstanceOf(IllegalArgumentException.class).hasMessageContaining("po_number");
	}

	private AttributeDefinition definition(String key, AttributeType type, boolean required,
			java.util.List<String> enumValues) {
		AttributeDefinition definition = new AttributeDefinition();
		definition.setTarget(AttributeTarget.TICKET);
		definition.setKey(key);
		definition.setLabel(key);
		definition.setType(type);
		definition.setRequired(required);
		definition.setEnumValues(enumValues);
		return definition;
	}
}
