package dev.alkolhar.servdesk.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Header-based API versioning. The version condition falls back to whatever the
 * class-level {@code @RequestMapping(version = "1")} declares.
 * <p>
 * {@code SetupController} is deliberately left unversioned: it is the pre-auth
 * bootstrap endpoint a client hits before anything else exists, so version
 * negotiation doesn't apply there.
 */
@Configuration
public class ApiVersioningConfig implements WebMvcConfigurer {

	@Override
	public void configureApiVersioning(ApiVersionConfigurer configurer) {
		configurer.useRequestHeader("X-API-Version").setDefaultVersion("1").detectSupportedVersions(true);
	}
}
