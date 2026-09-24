package dev.alkolhar.servdesk.directory;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonRepository extends JpaRepository<Person, Long> {

	Optional<Person> findByUsername(String username);

	Page<Person> findByRole(PersonRole role, Pageable pageable);

	/**
	 * Whether anyone <em>other than</em> {@code id} can still log in and administer
	 * this deployment: an Agent with no username, no password or
	 * {@code enabled = false} holds a directory row but cannot log in, so it does
	 * not keep the deployment reachable (issue #63). {@code Person}'s
	 * {@code @SQLRestriction} keeps soft-deleted rows out.
	 * <p>
	 * An existence check rather than a count: Postgres stops at the first matching
	 * row instead of scanning them all, and the question the invariant actually
	 * asks — "is there somebody else?" — needs no arithmetic about whether the
	 * person being removed is themselves in the total.
	 */
	boolean existsByRoleAndEnabledTrueAndUsernameIsNotNullAndPasswordIsNotNullAndIdNot(PersonRole role, Long id);
}
