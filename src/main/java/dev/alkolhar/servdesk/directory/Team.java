package dev.alkolhar.servdesk.directory;

import dev.alkolhar.servdesk.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "team")
@SQLDelete(sql = "UPDATE team SET deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
public class Team extends BaseEntity {

	@Column(nullable = false, unique = true, length = 100)
	private String name;

	@Column(length = 500)
	private @Nullable String description;

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public @Nullable String getDescription() {
		return description;
	}

	public void setDescription(@Nullable String description) {
		this.description = description;
	}
}
