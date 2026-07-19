package dev.alkolhar.servdesk.ticket.incident;

import dev.alkolhar.servdesk.common.MapsIdBaseEntity;
import dev.alkolhar.servdesk.ticket.Ticket;
import dev.alkolhar.servdesk.ticket.problem.Problem;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.jspecify.annotations.Nullable;

/**
 * An unplanned interruption or reduction in quality of a service — see
 * CONTEXT.md. Composes with a shared {@link Ticket} record via
 * {@code @OneToOne @MapsId} rather than extending it (ADR-0001);
 * {@link #displayNumber} is this subtype's own human-readable id
 * ({@code INC-000123}), drawn from {@code incident_number_seq}.
 * {@link #relatedProblem} is many-to-one (many Incidents can share one root
 * cause) and optional — not every Incident has an identified Problem yet.
 */
@Entity
@Table(name = "incident")
@SQLDelete(sql = "UPDATE incident SET deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
public class Incident extends MapsIdBaseEntity {

	@OneToOne
	@MapsId
	@JoinColumn(name = "id")
	private Ticket ticket;

	@Column(name = "display_number", nullable = false, unique = true, length = 20)
	private String displayNumber;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "related_problem_id")
	private @Nullable Problem relatedProblem;

	public Ticket getTicket() {
		return ticket;
	}

	public void setTicket(Ticket ticket) {
		this.ticket = ticket;
	}

	public String getDisplayNumber() {
		return displayNumber;
	}

	public void setDisplayNumber(String displayNumber) {
		this.displayNumber = displayNumber;
	}

	public @Nullable Problem getRelatedProblem() {
		return relatedProblem;
	}

	public void setRelatedProblem(@Nullable Problem relatedProblem) {
		this.relatedProblem = relatedProblem;
	}
}
