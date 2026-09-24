package dev.alkolhar.servdesk.directory;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonRepository extends JpaRepository<Person, Long> {

	Optional<Person> findByUsername(String username);

	Page<Person> findByRole(PersonRole role, Pageable pageable);

	/**
	 * Counts the people who can actually administer this deployment: an Agent with
	 * no username, no password or {@code enabled = false} holds a directory row but
	 * cannot log in, so it does not keep the deployment reachable (issue #63).
	 * {@code Person}'s {@code @SQLRestriction} keeps soft-deleted rows out.
	 */
	long countByRoleAndEnabledTrueAndUsernameIsNotNullAndPasswordIsNotNull(PersonRole role);
}
