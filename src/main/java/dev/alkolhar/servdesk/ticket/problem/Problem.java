package dev.alkolhar.servdesk.ticket.problem;

import dev.alkolhar.servdesk.common.MapsIdBaseEntity;
import dev.alkolhar.servdesk.ticket.Ticket;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * The root cause behind one or more Incidents — see CONTEXT.md. Composes with a
 * shared {@link Ticket} record via {@code @OneToOne @MapsId} rather than
 * extending it (ADR-0001); {@link #displayNumber} is this subtype's own
 * human-readable id ({@code PRB-000123}), drawn from
 * {@code problem_number_seq}.
 */
@Entity
@Table(name = "problem")
@SQLDelete(sql = "UPDATE problem SET deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
public class Problem extends MapsIdBaseEntity {

	@OneToOne
	@MapsId
	@JoinColumn(name = "id")
	private Ticket ticket;

	@Column(name = "display_number", nullable = false, unique = true, length = 20)
	private String displayNumber;

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
}
