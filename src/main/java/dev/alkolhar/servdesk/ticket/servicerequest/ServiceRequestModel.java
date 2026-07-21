package dev.alkolhar.servdesk.ticket.servicerequest;

import dev.alkolhar.servdesk.ticket.TicketStatus;
import java.time.Instant;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.hateoas.RepresentationModel;

@SuppressWarnings("NotNullFieldNotInitialized")
public class ServiceRequestModel extends RepresentationModel<ServiceRequestModel> {

	private Long id;
	private String displayNumber;
	private TicketStatus status;
	private String subject;
	private @Nullable String description;

	private Map<String, Object> attributes;
	private @Nullable Long categoryId;
	private @Nullable Long priorityId;
	private Long requesterId;
	private @Nullable Long assigneeId;
	private @Nullable Long teamId;
	private @Nullable Instant resolvedAt;
	private @Nullable Instant closedAt;

	private @Nullable Instant respondBy;

	private @Nullable Instant resolveBy;

	private @Nullable Instant firstRespondedAt;

	private @Nullable Instant responseBreachedAt;

	private @Nullable Instant resolutionBreachedAt;
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

	public String getDisplayNumber() {
		return displayNumber;
	}

	public void setDisplayNumber(String displayNumber) {
		this.displayNumber = displayNumber;
	}

	public TicketStatus getStatus() {
		return status;
	}

	public void setStatus(TicketStatus status) {
		this.status = status;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public @Nullable String getDescription() {
		return description;
	}

	public void setDescription(@Nullable String description) {
		this.description = description;
	}

	public Map<String, Object> getAttributes() {
		return attributes;
	}

	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}

	public @Nullable Long getCategoryId() {
		return categoryId;
	}

	public void setCategoryId(@Nullable Long categoryId) {
		this.categoryId = categoryId;
	}

	public @Nullable Long getPriorityId() {
		return priorityId;
	}

	public void setPriorityId(@Nullable Long priorityId) {
		this.priorityId = priorityId;
	}

	public Long getRequesterId() {
		return requesterId;
	}

	public void setRequesterId(Long requesterId) {
		this.requesterId = requesterId;
	}

	public @Nullable Long getAssigneeId() {
		return assigneeId;
	}

	public void setAssigneeId(@Nullable Long assigneeId) {
		this.assigneeId = assigneeId;
	}

	public @Nullable Long getTeamId() {
		return teamId;
	}

	public void setTeamId(@Nullable Long teamId) {
		this.teamId = teamId;
	}

	public @Nullable Instant getResolvedAt() {
		return resolvedAt;
	}

	public void setResolvedAt(@Nullable Instant resolvedAt) {
		this.resolvedAt = resolvedAt;
	}

	public @Nullable Instant getClosedAt() {
		return closedAt;
	}

	public void setClosedAt(@Nullable Instant closedAt) {
		this.closedAt = closedAt;
	}

	public @Nullable Instant getRespondBy() {
		return respondBy;
	}

	public void setRespondBy(@Nullable Instant respondBy) {
		this.respondBy = respondBy;
	}

	public @Nullable Instant getResolveBy() {
		return resolveBy;
	}

	public void setResolveBy(@Nullable Instant resolveBy) {
		this.resolveBy = resolveBy;
	}

	public @Nullable Instant getFirstRespondedAt() {
		return firstRespondedAt;
	}

	public void setFirstRespondedAt(@Nullable Instant firstRespondedAt) {
		this.firstRespondedAt = firstRespondedAt;
	}

	public @Nullable Instant getResponseBreachedAt() {
		return responseBreachedAt;
	}

	public void setResponseBreachedAt(@Nullable Instant responseBreachedAt) {
		this.responseBreachedAt = responseBreachedAt;
	}

	public @Nullable Instant getResolutionBreachedAt() {
		return resolutionBreachedAt;
	}

	public void setResolutionBreachedAt(@Nullable Instant resolutionBreachedAt) {
		this.resolutionBreachedAt = resolutionBreachedAt;
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
