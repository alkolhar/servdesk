package dev.alkolhar.servdesk.classification;

import dev.alkolhar.servdesk.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

/**
 * The other axis of the Impact x Urgency priority matrix (see
 * {@link PriorityDefinition}) — how quickly a ticket's underlying issue needs
 * addressing. Same shape as {@link Priority}, which this entity now feeds
 * rather than being set directly.
 */
@Entity
@Table(name = "urgency")
@SQLDelete(sql = "UPDATE urgency SET deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
public class Urgency extends BaseEntity {

	@Column(nullable = false, unique = true, length = 50)
	private String name;

	/**
	 * Lower values sort first / are more severe.
	 */
	@Column(name = "sort_order", nullable = false)
	private int sortOrder;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public int getSortOrder() {
		return sortOrder;
	}

	public void setSortOrder(int sortOrder) {
		this.sortOrder = sortOrder;
	}
}
