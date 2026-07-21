package dev.alkolhar.servdesk.ticket.change;

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
 * A planned modification to infrastructure or a service — see CONTEXT.md.
 * Composes with a shared {@link Ticket} record via {@code @OneToOne @MapsId}
 * rather than extending it (ADR-0001); {@link #displayNumber} is this subtype's
 * own human-readable id ({@code RFC-000123}, "Request for Change"), drawn from
 * {@code change_number_seq}.
 * <p>
 * The table is named {@code change_request}, not {@code change}: originally
 * because {@code CHANGE} is a reserved word in MariaDB's grammar (the schema's
 * first home), kept on PostgreSQL because it's the clearer name anyway.
 */
@Entity
@Table(name = "change_request")
@SQLDelete(sql = "UPDATE change_request SET deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
public class Change extends MapsIdBaseEntity {

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
