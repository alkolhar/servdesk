package dev.alkolhar.servdesk.sla;

import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.quartz.autoconfigure.QuartzAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Covers the one thing {@link SlaLifecycleTest} deliberately cannot: the wiring
 * between the Quartz tick and {@link SlaScanService}. The lifecycle test calls
 * the service directly — fast and deterministic, but it left a job that never
 * received its dependency looking perfectly healthy (issue #56).
 * <p>
 * The scanner's own behavior is not re-tested here, only that a
 * <em>scheduled</em> tick arrives at the service at all, so the context is
 * narrowed to the scheduling beans plus Quartz autoconfiguration and the
 * service is a mock. That keeps this off the database (and so off Docker)
 * entirely, and keeps the assertion pointed at the wiring rather than the scan.
 */
@SpringBootTest(classes = SlaSchedulingConfig.class, properties = "servdesk.sla.scan-interval-seconds=1")
@ImportAutoConfiguration(QuartzAutoConfiguration.class)
class SlaScanSchedulingTest {

	@MockitoBean
	private SlaScanService slaScanService;

	@Test
	void theScheduledJobReachesTheScanService() {
		await().atMost(Duration.ofSeconds(10))
				.untilAsserted(() -> verify(slaScanService, atLeastOnce()).scan(any(Instant.class)));
	}
}
