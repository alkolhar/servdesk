package dev.alkolhar.servdesk.common;

import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import org.jspecify.annotations.Nullable;

/**
 * Base for entities that share another entity's primary key via
 * {@code @OneToOne @MapsId} rather than generating their own — e.g. a ticket
 * subtype (Incident, Problem, ...) sharing the shared {@code Ticket} record's
 * id (see ADR-0001). {@link BaseEntity#id} can't be reused here: it carries
 * {@code @GeneratedValue(IDENTITY)}, which conflicts with {@code @MapsId}'s
 * requirement that the id be assigned from the owning association instead of
 * its own generator. The id is set by Hibernate when the {@code @MapsId}
 * association is persisted, never assigned directly.
 */
@MappedSuperclass
public abstract class MapsIdBaseEntity extends AuditableEntity {

	@Id
	private @Nullable Long id;

	public @Nullable Long getId() {
		return id;
	}
}
