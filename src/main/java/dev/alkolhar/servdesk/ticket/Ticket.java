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
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.jspecify.annotations.Nullable;

/**
 * Persistence-only type — see the note on
 * {@link dev.alkolhar.servdesk.directory.Person} for why this is never bound to
 * a request body directly. Requests use {@link TicketCreateRequest}/
 * {@link TicketUpdateRequest}; responses are {@link TicketModel}, assembled by
 * {@link TicketModelAssembler}.
 */
@Entity
@Table(name = "ticket")
@SQLDelete(sql = "UPDATE ticket SET deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
public class Ticket extends BaseEntity {

	@Column(name = "ticket_number", nullable = false, unique = true, length = 20)
	private String ticketNumber;

	@Column(nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private TicketType type;

	@Column(nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private TicketStatus status = TicketStatus.OPEN;

	@Column(nullable = false)
	private String subject;

	@Lob
	private @Nullable String description;

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

	public String getTicketNumber() {
		return ticketNumber;
	}

	public void setTicketNumber(String ticketNumber) {
		this.ticketNumber = ticketNumber;
	}

	public TicketType getType() {
		return type;
	}

	public void setType(TicketType type) {
		this.type = type;
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
