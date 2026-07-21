package dev.alkolhar.servdesk.customfield;

import dev.alkolhar.servdesk.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.util.List;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;

/**
 * An admin-editable declaration that a target aggregate accepts a custom-field
 * key, and what a valid value for it looks like. {@link #key} and {@link #type}
 * are immutable after creation (enforced in
 * {@code AttributeDefinitionCommandService}) — changing either would silently
 * invalidate values already stored on tickets. Soft-deleting a definition
 * leaves stored values in place; the key just stops being accepted on
 * subsequent writes (validation is write-time only, see
 * {@code AttributeValidator}).
 * <p>
 * Columns {@code attr_key}/{@code attr_type}, not {@code key}/{@code type}:
 * both bare names are keywords in enough SQL dialects and tools to be worth
 * avoiding.
 */
@Entity
@Table(name = "attribute_definition")
@SQLDelete(sql = "UPDATE attribute_definition SET deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
public class AttributeDefinition extends BaseEntity {

	@Column(name = "target_type", nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private AttributeTarget target;

	@Column(name = "attr_key", nullable = false, length = 100)
	private String key;

	@Column(nullable = false, length = 150)
	private String label;

	@Column(name = "attr_type", nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private AttributeType type;

	@Column(nullable = false)
	private boolean required = false;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(name = "enum_values")
	private @Nullable List<String> enumValues;

	public AttributeTarget getTarget() {
		return target;
	}

	public void setTarget(AttributeTarget target) {
		this.target = target;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public AttributeType getType() {
		return type;
	}

	public void setType(AttributeType type) {
		this.type = type;
	}

	public boolean isRequired() {
		return required;
	}

	public void setRequired(boolean required) {
		this.required = required;
	}

	public @Nullable List<String> getEnumValues() {
		return enumValues;
	}

	public void setEnumValues(@Nullable List<String> enumValues) {
		this.enumValues = enumValues;
	}
}
