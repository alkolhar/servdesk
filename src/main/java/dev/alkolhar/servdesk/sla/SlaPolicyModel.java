package dev.alkolhar.servdesk.sla;

import java.time.Instant;
import org.jspecify.annotations.Nullable;
import org.springframework.hateoas.RepresentationModel;

@SuppressWarnings("NotNullFieldNotInitialized")
public class SlaPolicyModel extends RepresentationModel<SlaPolicyModel> {

	private Long id;
	private Long priorityId;
	private @Nullable Integer responseMinutes;
	private @Nullable Integer resolutionMinutes;
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

	public Long getPriorityId() {
		return priorityId;
	}

	public void setPriorityId(Long priorityId) {
		this.priorityId = priorityId;
	}

	public @Nullable Integer getResponseMinutes() {
		return responseMinutes;
	}

	public void setResponseMinutes(@Nullable Integer responseMinutes) {
		this.responseMinutes = responseMinutes;
	}

	public @Nullable Integer getResolutionMinutes() {
		return resolutionMinutes;
	}

	public void setResolutionMinutes(@Nullable Integer resolutionMinutes) {
		this.resolutionMinutes = resolutionMinutes;
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
