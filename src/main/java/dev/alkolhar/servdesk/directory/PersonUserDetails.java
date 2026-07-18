package dev.alkolhar.servdesk.directory;

import java.util.Collection;
import java.util.Set;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Adapts a login-capable {@link Person} (one with a
 * {@code username}/{@code password} set) to Spring Security's authentication
 * model, so {@link Person} itself can stay a plain JPA entity.
 */
public class PersonUserDetails implements UserDetails {

	private final Person person;

	public PersonUserDetails(Person person) {
		this.person = person;
	}

	public Person getPerson() {
		return person;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return Set.of(new SimpleGrantedAuthority("ROLE_" + person.getRole().name()));
	}

	@Override
	public String getPassword() {
		return person.getPassword();
	}

	@Override
	public String getUsername() {
		return person.getUsername();
	}

	@Override
	public boolean isEnabled() {
		return person.isEnabled();
	}
}
