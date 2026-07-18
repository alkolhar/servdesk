package dev.alkolhar.servdesk.directory;

import dev.alkolhar.servdesk.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.jspecify.annotations.Nullable;

/**
 * A single identity for both agents and customers. {@link #role} distinguishes
 * which kind of person this is; {@link #username}/{@link #password} are only
 * populated for people who can log in (agents today, potentially customers
 * later via a self-service portal). Finer-grained permissions (e.g. admin vs
 * regular agent) are handled by Spring Security authorities, not here.
 * <p>
 * This is a persistence-only type: it is never bound to a request body or
 * serialized directly in a response. Input validation lives on the request DTOs
 * ({@link PersonCreateRequest}/ {@link PersonUpdateRequest}); API responses are
 * {@link PersonModel}, assembled by {@link PersonModelAssembler}.
 */
@Entity
@Table(name = "person")
@SQLDelete(sql = "UPDATE person SET deleted_at = CURRENT_TIMESTAMP WHERE id = ? AND version = ?")
@SQLRestriction("deleted_at IS NULL")
public class Person extends BaseEntity {

	@Column(nullable = false, length = 20)
	@Enumerated(EnumType.STRING)
	private PersonRole role;

	@Column(nullable = false, length = 200)
	private String name;

	@Column(nullable = false, unique = true)
	private String email;

	@Column(length = 50)
	private @Nullable String phone;

	@Column(unique = true, length = 100)
	private @Nullable String username;

	private @Nullable String password;

	@Column(nullable = false)
	private boolean enabled = true;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "team_id")
	private @Nullable Team team;

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

	public @Nullable String getPassword() {
		return password;
	}

	public void setPassword(@Nullable String password) {
		this.password = password;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public @Nullable Team getTeam() {
		return team;
	}

	public void setTeam(@Nullable Team team) {
		this.team = team;
	}
}
