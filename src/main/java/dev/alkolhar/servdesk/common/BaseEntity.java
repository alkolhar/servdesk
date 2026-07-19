package dev.alkolhar.servdesk.common;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import org.jspecify.annotations.Nullable;

/**
 * Base for entities whose primary key is database-generated. See
 * {@link MapsIdBaseEntity} for entities that instead share another entity's
 * primary key via {@code @MapsId}.
 */
@MappedSuperclass
public abstract class BaseEntity extends AuditableEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private @Nullable Long id;

	public @Nullable Long getId() {
		return id;
	}
}
