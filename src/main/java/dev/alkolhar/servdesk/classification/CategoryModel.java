package dev.alkolhar.servdesk.classification;

import java.time.Instant;
import org.jspecify.annotations.Nullable;
import org.springframework.hateoas.RepresentationModel;

@SuppressWarnings("NotNullFieldNotInitialized")
public class CategoryModel extends RepresentationModel<CategoryModel> {

	private Long id;
	private String name;
	private @Nullable Long parentId;
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

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public @Nullable Long getParentId() {
		return parentId;
	}

	public void setParentId(@Nullable Long parentId) {
		this.parentId = parentId;
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
