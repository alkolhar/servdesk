package dev.alkolhar.servdesk.sla;

import dev.alkolhar.servdesk.classification.Priority;
import dev.alkolhar.servdesk.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.jspecify.annotations.Nullable;

/**
 * Response/resolution targets for tickets of one {@link Priority} — at most one
 * live policy per priority (partial unique index). Either minutes value may be
 * null: no target of that kind. Mapped {@code @ManyToOne} for the usual
 * proxy-id reasons, the uniqueness lives in the database.
 */
@Entity
@Table(name = "sla_policy")
@SQLDelete(sql = "UPDATE sla_policy SET deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
public class SlaPolicy extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "priority_id", nullable = false)
	private Priority priority;

	@Column(name = "response_minutes")
	private @Nullable Integer responseMinutes;

	@Column(name = "resolution_minutes")
	private @Nullable Integer resolutionMinutes;

	public Priority getPriority() {
		return priority;
	}

	public void setPriority(Priority priority) {
		this.priority = priority;
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
}
