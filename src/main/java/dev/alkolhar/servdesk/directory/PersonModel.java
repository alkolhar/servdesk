package dev.alkolhar.servdesk.directory;

import java.time.Instant;
import org.jspecify.annotations.Nullable;
import org.springframework.hateoas.RepresentationModel;

/**
 * API representation of a {@link Person}. Deliberately has no {@code password}
 * field — it never leaves the service, encoded or not.
 */
@SuppressWarnings("NotNullFieldNotInitialized")
public class PersonModel extends RepresentationModel<PersonModel> {

	private Long id;
	private PersonRole role;
	private String name;
	private String email;
	private @Nullable String phone;
	private @Nullable String username;
	private boolean enabled;
	private @Nullable Long teamId;
	private Instant createdAt;
	private Instant updatedAt;
	private @Nullable String createdBy;
	private @Nullable String updatedBy;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public PersonRole getRole() {
		return role;
	}

	public void setRole(PersonRole role) {
		this.role = role;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getEmail() {
		return email;
	}

	public void setEmail(String email) {
		this.email = email;
	}

	public @Nullable String getPhone() {
		return phone;
	}

	public void setPhone(@Nullable String phone) {
		this.phone = phone;
	}

	public @Nullable String getUsername() {
		return username;
	}

	public void setUsername(@Nullable String username) {
		this.username = username;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public @Nullable Long getTeamId() {
		return teamId;
	}

	public void setTeamId(@Nullable Long teamId) {
		this.teamId = teamId;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

	public Instant getUpdatedAt() {
		return updatedAt;
	}

	public void setUpdatedAt(Instant updatedAt) {
		this.updatedAt = updatedAt;
	}

	public @Nullable String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(@Nullable String createdBy) {
		this.createdBy = createdBy;
	}

	public @Nullable String getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(@Nullable String updatedBy) {
		this.updatedBy = updatedBy;
	}
}
