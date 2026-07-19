package dev.alkolhar.servdesk.classification;

import java.time.Instant;
import org.jspecify.annotations.Nullable;
import org.springframework.hateoas.RepresentationModel;

@SuppressWarnings("NotNullFieldNotInitialized")
public class PriorityDefinitionModel extends RepresentationModel<PriorityDefinitionModel> {

	private Long id;
	private Long impactId;
	private Long urgencyId;
	private Long priorityId;
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

	public Long getImpactId() {
		return impactId;
	}

	public void setImpactId(Long impactId) {
		this.impactId = impactId;
	}

	public Long getUrgencyId() {
		return urgencyId;
	}

	public void setUrgencyId(Long urgencyId) {
		this.urgencyId = urgencyId;
	}

	public Long getPriorityId() {
		return priorityId;
	}

	public void setPriorityId(Long priorityId) {
		this.priorityId = priorityId;
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
