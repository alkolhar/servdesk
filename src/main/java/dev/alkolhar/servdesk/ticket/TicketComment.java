package dev.alkolhar.servdesk.ticket;

import dev.alkolhar.servdesk.common.BaseEntity;
import dev.alkolhar.servdesk.directory.Person;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "ticket_comment")
@SQLDelete(sql = "UPDATE ticket_comment SET deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
public class TicketComment extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "ticket_id", nullable = false)
	private Ticket ticket;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "author_id", nullable = false)
	private Person author;

	/**
	 * {@code LONGVARCHAR}, not {@code @Lob}: on PostgreSQL Hibernate maps a
	 * {@code @Lob String} to an {@code oid} large object (a separate row in
	 * {@code pg_largeobject} reached via streaming API), while this column is a
	 * plain unbounded {@code TEXT} — {@code ddl-auto=validate} checks the mapping
	 * against it.
	 */
	@JdbcTypeCode(SqlTypes.LONGVARCHAR)
	@Column(nullable = false)
	private String body;

	/**
	 * Internal notes are visible to agents only; non-internal comments are visible
	 * to the requester too.
	 */
	@Column(nullable = false)
	private boolean internal = false;

	public Ticket getTicket() {
		return ticket;
	}

	public void setTicket(Ticket ticket) {
		this.ticket = ticket;
	}

	public Person getAuthor() {
		return author;
	}

	public void setAuthor(Person author) {
		this.author = author;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public boolean isInternal() {
		return internal;
	}

	public void setInternal(boolean internal) {
		this.internal = internal;
	}
}
