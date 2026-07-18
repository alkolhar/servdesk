package dev.alkolhar.servdesk.directory;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class PersonUserDetailsService implements UserDetailsService {

	private final PersonRepository personRepository;

	public PersonUserDetailsService(PersonRepository personRepository) {
		this.personRepository = personRepository;
	}

	@Override
	public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
		Person person = personRepository.findByUsername(username)
				.orElseThrow(() -> new UsernameNotFoundException("No such user: " + username));
		if (person.getPassword() == null) {
			// username set without a password isn't login-capable; treat like any other
			// unknown user
			throw new UsernameNotFoundException("No such user: " + username);
		}
		return new PersonUserDetails(person);
	}
}
