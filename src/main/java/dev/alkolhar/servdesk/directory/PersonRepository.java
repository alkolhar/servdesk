package dev.alkolhar.servdesk.directory;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonRepository extends JpaRepository<Person, Long> {

	Optional<Person> findByUsername(String username);

	Page<Person> findByRole(PersonRole role, Pageable pageable);
}
