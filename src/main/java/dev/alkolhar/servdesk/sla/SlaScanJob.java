package dev.alkolhar.servdesk.sla;

import java.time.Instant;
import org.quartz.JobExecutionContext;
import org.springframework.scheduling.quartz.QuartzJobBean;

/**
 * Thin Quartz shell around {@link SlaScanService} — all logic (and all testing)
 * lives in the service; Quartz only provides the clock tick.
 * <p>
 * The constructor <em>is</em> the wiring. Boot installs a plain
 * {@code SpringBeanJobFactory} with the {@code ApplicationContext} set
 * ({@code QuartzAutoConfiguration}), and its {@code createJobInstance} runs the
 * job class through {@code AutowireCapableBeanFactory.createBean(...)} — full
 * bean creation, so constructor arguments are resolved from the context and
 * {@link SlaSchedulingConfig} needs no special registration.
 * <p>
 * Taking the dependency here rather than through a setter is what makes issue
 * #56 unrepeatable. The original bare setter was never called — the factory's
 * remaining property population draws only on the scheduler context and the
 * {@code JobDataMap}, neither of which holds this bean — so the job looked
 * wired and ticked with a null field, silently, for the life of the deployment.
 * A constructor argument cannot be skipped that quietly: the instance either
 * receives the service or is never created.
 */
public class SlaScanJob extends QuartzJobBean {

	private final SlaScanService slaScanService;

	public SlaScanJob(SlaScanService slaScanService) {
		this.slaScanService = slaScanService;
	}

	@Override
	protected void executeInternal(JobExecutionContext context) {
		slaScanService.scan(Instant.now());
	}

}
