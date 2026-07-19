package dev.alkolhar.servdesk.ticket.servicerequest;

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
 * A routine request for something (e.g. a new account, access to a system), not
 * a report of something broken — see CONTEXT.md. Composes with a shared
 * {@link Ticket} record via {@code @OneToOne @MapsId} rather than extending it
 * (ADR-0001); {@link #displayNumber} is this subtype's own human-readable id
 * ({@code REQ-000123}), drawn from {@code service_request_number_seq}.
 */
@Entity
@Table(name = "service_request")
@SQLDelete(sql = "UPDATE service_request SET deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
public class ServiceRequest extends MapsIdBaseEntity {

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
