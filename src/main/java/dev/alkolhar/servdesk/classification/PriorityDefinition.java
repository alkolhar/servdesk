package dev.alkolhar.servdesk.classification;

import dev.alkolhar.servdesk.common.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * One cell of the Impact x Urgency priority matrix: maps a single
 * ({@link Impact}, {@link Urgency}) pair to the {@link Priority} a ticket
 * carrying that pair should be given. Unique on the (impact, urgency)
 * combination — see the migration's
 * {@code uk_priority_definition_impact_urgency} constraint — but multiple
 * combinations may point at the same {@code Priority} (e.g. both Low/Low and
 * Medium/Low could resolve to "Low"). Ticket subtypes never set
 * {@code Priority} directly; it's derived from this table by
 * {@code AbstractTicketSubtypeCommandService} — see CONTEXT.md.
 */
@Entity
@Table(name = "priority_definition")
@SQLDelete(sql = "UPDATE priority_definition SET deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
public class PriorityDefinition extends BaseEntity {

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "impact_id", nullable = false)
	private Impact impact;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "urgency_id", nullable = false)
	private Urgency urgency;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "priority_id", nullable = false)
	private Priority priority;

	public Impact getImpact() {
		return impact;
	}

	public void setImpact(Impact impact) {
		this.impact = impact;
	}

	public Urgency getUrgency() {
		return urgency;
	}

	public void setUrgency(Urgency urgency) {
		this.urgency = urgency;
	}

	public Priority getPriority() {
		return priority;
	}

	public void setPriority(Priority priority) {
		this.priority = priority;
	}
}
