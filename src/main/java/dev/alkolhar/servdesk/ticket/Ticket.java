package dev.alkolhar.servdesk.ticket;

import dev.alkolhar.servdesk.classification.Category;
import dev.alkolhar.servdesk.classification.Priority;
import dev.alkolhar.servdesk.common.BaseEntity;
import dev.alkolhar.servdesk.directory.Person;
import dev.alkolhar.servdesk.directory.Team;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;
import org.jspecify.annotations.Nullable;

/**
 * Fields shared by every ticket subtype (Incident, Problem, Change, Service
 * Request) — see ADR-0001. A subtype entity composes with this one via
 * {@code @OneToOne @MapsId}, sharing this record's primary key rather than
 * extending it; {@code type}/a display number live on the subtype, not here,
 * since a subtype's own existence already says what kind of ticket it is.
 * Persistence-only type — see the note on
 * {@link dev.alkolhar.servdesk.directory.Person} for why this is never bound to
 * a request body or serialized directly in a response.
 */
@Entity
@Table(name = "ticket")
@SQLDelete(sql = "UPDATE ticket SET deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
public class Ticket extends BaseEntity {

	@Column(nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private TicketStatus status = TicketStatus.OPEN;

	@Column(nullable = false)
	private String subject;

	/**
	 * {@code LONGVARCHAR}, not {@code @Lob}: on PostgreSQL Hibernate maps a
	 * {@code @Lob String} to an {@code oid} large object (a separate row in
	 * {@code pg_largeobject} reached via streaming API), while this column is a
	 * plain unbounded {@code TEXT}.
	 */
	@JdbcTypeCode(SqlTypes.LONGVARCHAR)
	private @Nullable String description;

	/**
	 * Customer-defined custom-field values (issue #29): a {@code jsonb} column
	 * (GIN-indexed, see {@code V1__init_schema.sql}) keyed by
	 * {@code AttributeDefinition.key}. Always written through
	 * {@code AbstractTicketSubtypeCommandService}, which validates the full map via
	 * {@code AttributeValidator} first — never mutated elsewhere.
	 */
	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false)
	private Map<String, Object> attributes = new HashMap<>();

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "category_id")
	private @Nullable Category category;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "priority_id")
	private @Nullable Priority priority;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "requester_id", nullable = false)
	private Person requester;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "assignee_id")
	private @Nullable Person assignee;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "team_id")
	private @Nullable Team team;

	private @Nullable Instant resolvedAt;

	private @Nullable Instant closedAt;

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

	public @Nullable Category getCategory() {
		return category;
	}

	public void setCategory(@Nullable Category category) {
		this.category = category;
	}

	public @Nullable Priority getPriority() {
		return priority;
	}

	public void setPriority(@Nullable Priority priority) {
		this.priority = priority;
	}

	public Person getRequester() {
		return requester;
	}

	public void setRequester(Person requester) {
		this.requester = requester;
	}

	public @Nullable Person getAssignee() {
		return assignee;
	}

	public void setAssignee(@Nullable Person assignee) {
		this.assignee = assignee;
	}

	public @Nullable Team getTeam() {
		return team;
	}

	public void setTeam(@Nullable Team team) {
		this.team = team;
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
}
