package dev.alkolhar.servdesk.customfield;

import java.time.Instant;
import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.hateoas.RepresentationModel;

@SuppressWarnings("NotNullFieldNotInitialized")
public class AttributeDefinitionModel extends RepresentationModel<AttributeDefinitionModel> {

	private Long id;
	private AttributeTarget target;
	private String key;
	private String label;
	private AttributeType type;
	private boolean required;
	private @Nullable List<String> enumValues;
	private Instant createdAt;
	private Instant updatedAt;
	private @Nullable String createdBy;
	private @Nullable String updatedBy;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

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

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}

	public @Nullable String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(@Nullable String createdBy) {
		this.createdBy = createdBy;
	}

	public @Nullable String getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(@Nullable String updatedBy) {
		this.updatedBy = updatedBy;
	}
}
