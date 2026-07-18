package dev.alkolhar.servdesk.config;

import java.util.Optional;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Backs {@code createdBy}/{@code updatedBy} on
 * {@link dev.alkolhar.servdesk.common.BaseEntity}. Spring Security's
 * {@code AnonymousAuthenticationFilter} is active by default (never disabled in
 * {@link SecurityConfig}), so even the permitAll {@code /api/setup} flow has an
 * authenticated principal ({@code "anonymousUser"}) — the
 * empty-{@link Optional} branch below only matters for writes that happen with
 * no {@link org.springframework.security.core.context.SecurityContext} at all,
 * e.g. a future Quartz job running outside a request.
 */
@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAware")
public class JpaAuditingConfig {

	@Bean
	public AuditorAware<String> auditorAware() {
		return () -> {
			Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
			if (authentication == null || !authentication.isAuthenticated()) {
				return Optional.empty();
			}
			return Optional.of(authentication.getName());
		};
	}
}
