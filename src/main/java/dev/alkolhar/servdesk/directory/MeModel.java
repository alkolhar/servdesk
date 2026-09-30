package dev.alkolhar.servdesk.directory;

import org.jspecify.annotations.Nullable;
import org.springframework.hateoas.RepresentationModel;

/**
 * The logged-in person, as the SPA starts up from it (ADR-0005). Deliberately
 * its own model rather than {@link PersonModel}: this is "who am I", open to
 * every authenticated caller, while {@link PersonModel}'s self link points into
 * the Agent-only person directory. The {@code admin} flag and team memberships
 * ADR-0005 lists join this model with the slices that introduce them.
 */
@SuppressWarnings("NotNullFieldNotInitialized")
public class MeModel extends RepresentationModel<MeModel> {

	private Long id;
	private PersonRole role;
	private String name;
	private String email;
	private @Nullable String username;

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

	public @Nullable String getUsername() {
		return username;
	}

	public void setUsername(@Nullable String username) {
		this.username = username;
	}

}
