package dev.alkolhar.servdesk.sla;

import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * The first actual Quartz consumer (the starter had been waiting on the
 * classpath): one repeating trigger for {@link SlaScanJob}. Interval via
 * {@code servdesk.sla.scan-interval-seconds} (default 60) — integration tests
 * don't rely on it, they call {@link SlaScanService} directly. Default
 * in-memory job store: a missed tick is irrelevant, the next pass catches the
 * same breaches (the stamps make that idempotent).
 */
@Configuration
public class SlaSchedulingConfig {

	@Bean
	JobDetail slaScanJobDetail() {
		return JobBuilder.newJob(SlaScanJob.class).withIdentity("slaScanJob").storeDurably().build();
	}

	@Bean
	Trigger slaScanTrigger(JobDetail slaScanJobDetail,
			@Value("${servdesk.sla.scan-interval-seconds:60}") int intervalSeconds) {
		return TriggerBuilder.newTrigger().forJob(slaScanJobDetail).withIdentity("slaScanTrigger")
				.withSchedule(
						SimpleScheduleBuilder.simpleSchedule().withIntervalInSeconds(intervalSeconds).repeatForever())
				.build();
	}
}
