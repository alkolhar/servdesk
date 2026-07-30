package dev.alkolhar.servdesk.sla;

import java.time.Instant;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;

/**
 * Thin Quartz shell around {@link SlaScanService} — all logic (and all testing)
 * lives in the service; Quartz only provides the clock tick. Spring's
 * {@code SpringBeanJobFactory} injects the service.
 */
public class SlaScanJob extends QuartzJobBean {

	private SlaScanService slaScanService;

	public void setSlaScanService(SlaScanService slaScanService) {
		this.slaScanService = slaScanService;
	}

	@Override
	protected void executeInternal(JobExecutionContext context) {
		slaScanService.scan(Instant.now());
	}
}
