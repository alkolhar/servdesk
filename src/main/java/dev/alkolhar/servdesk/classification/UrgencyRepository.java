package dev.alkolhar.servdesk.classification;

import org.springframework.data.jpa.repository.JpaRepository;

public interface UrgencyRepository extends JpaRepository<Urgency, Long> {
}
