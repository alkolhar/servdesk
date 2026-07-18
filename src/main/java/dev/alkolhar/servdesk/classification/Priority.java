package dev.alkolhar.servdesk.classification;

import dev.alkolhar.servdesk.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;

@Entity
@Table(name = "priority")
@SQLDelete(sql = "UPDATE priority SET deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
public class Priority extends BaseEntity {

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
