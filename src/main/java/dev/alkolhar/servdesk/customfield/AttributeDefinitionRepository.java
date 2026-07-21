package dev.alkolhar.servdesk.customfield;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttributeDefinitionRepository extends JpaRepository<AttributeDefinition, Long> {

	List<AttributeDefinition> findByTargetOrderByKeyAsc(AttributeTarget target);

	boolean existsByTargetAndKey(AttributeTarget target, String key);
}
