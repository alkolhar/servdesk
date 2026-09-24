package dev.alkolhar.servdesk.sla;

import java.time.Instant;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

/**
 * Thin Quartz shell around {@link SlaScanService} — all logic (and all testing)
 * lives in the service; Quartz only provides the clock tick.
 * <p>
 * The {@code @Autowired} on the setter is load-bearing: Boot's
 * {@code AutowireCapableBeanJobFactory} injects a job instance by calling
 * {@code beanFactory.autowireBean(job)}, which drives <em>annotation-based</em>
 * injection only. {@code SpringBeanJobFactory}'s own property population comes
 * from the {@code JobDataMap} and the scheduler context, neither of which holds
 * this bean, so an unannotated setter is simply never called and the field
 * stays null (issue #56).
 */
public class SlaScanJob extends QuartzJobBean {

	private SlaScanService slaScanService;

	@Autowired
	public void setSlaScanService(SlaScanService slaScanService) {
		this.slaScanService = slaScanService;
	}

	@Override
	protected void executeInternal(JobExecutionContext context) {
		slaScanService.scan(Instant.now());
	}
}
