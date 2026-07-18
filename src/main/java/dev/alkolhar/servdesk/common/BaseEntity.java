package dev.alkolhar.servdesk.common;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import java.time.Instant;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.jspecify.annotations.Nullable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class BaseEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private @Nullable Long id;

	@CreationTimestamp
	private @Nullable Instant createdAt;

	@UpdateTimestamp
	private @Nullable Instant updatedAt;

	@CreatedBy
	@Column(name = "created_by", updatable = false)
	private @Nullable String createdBy;

	@LastModifiedBy
	@Column(name = "updated_by")
	private @Nullable String updatedBy;

	/**
	 * Soft-delete marker. Never set directly from application code — each concrete
	 * entity's {@code @SQLDelete} override sets this at the database level in place
	 * of an actual row deletion, and {@code @SQLRestriction("deleted_at IS NULL")}
	 * keeps soft-deleted rows out of every normal query (including association
	 * fetches).
	 */
	@Column(name = "deleted_at")
	private @Nullable Instant deletedAt;

	@Version
	private @Nullable Long version;

	public @Nullable Long getId() {
		return id;
	}

	public @Nullable Instant getCreatedAt() {
		return createdAt;
	}

	public @Nullable Instant getUpdatedAt() {
		return updatedAt;
	}

	public @Nullable String getCreatedBy() {
		return createdBy;
	}

	public @Nullable String getUpdatedBy() {
		return updatedBy;
	}

	public @Nullable Instant getDeletedAt() {
		return deletedAt;
	}

	public boolean isDeleted() {
		return deletedAt != null;
	}

	public @Nullable Long getVersion() {
		return version;
	}
}
