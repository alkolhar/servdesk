package dev.alkolhar.servdesk.directory;

import dev.alkolhar.servdesk.common.exception.NotFoundException;
import org.jspecify.annotations.Nullable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class PersonQueryService {

	private final PersonRepository personRepository;

	public PersonQueryService(PersonRepository personRepository) {
		this.personRepository = personRepository;
	}

	public Page<Person> findAll(@Nullable PersonRole role, Pageable pageable) {
		return role == null ? personRepository.findAll(pageable) : personRepository.findByRole(role, pageable);
	}

	public Person findById(Long id) {
		return personRepository.findById(id).orElseThrow(() -> new NotFoundException("Person " + id + " not found"));
	}

	public boolean isSetupRequired() {
		return personRepository.count() == 0;
	}
}
