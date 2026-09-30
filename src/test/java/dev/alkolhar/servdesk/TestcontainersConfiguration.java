package dev.alkolhar.servdesk;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@TestConfiguration(proxyBeanMethods = false)
public class TestcontainersConfiguration {

	/**
	 * PostgreSQL 17.11 — the same image docker-compose.yml and the contract-tests
	 * job in ci.yml pin; keep all three in step. 17, not 18, because Flowable 8's
	 * CI covers 14–17 (ADR-0006). Digest only, no tag: {@link DockerImageName}
	 * rejects the {@code tag@digest} form the other two use. Bumped by hand — no
	 * Dependabot ecosystem reads an image string in Java source.
	 */
	public static final String POSTGRES_IMAGE = "postgres@sha256:d74eeac9a635390a49bc21bd49fccd973de707e2a53a76ac49b552b8712ec46f";

	@Bean
	@ServiceConnection
	public PostgreSQLContainer postgresContainer() {
		return new PostgreSQLContainer(DockerImageName.parse(POSTGRES_IMAGE));
	}

}
