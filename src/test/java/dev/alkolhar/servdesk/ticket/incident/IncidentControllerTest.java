package dev.alkolhar.servdesk.ticket.incident;

import dev.alkolhar.servdesk.ticket.AbstractTicketSubtypeControllerTest;

class IncidentControllerTest extends AbstractTicketSubtypeControllerTest {

	@Override
	protected String basePath() {
		return "/api/incidents";
	}

	@Override
	protected String expectedDisplayNumberPrefix() {
		return "INC-";
	}

	/** Since #110: the Incident lifecycle is {@code ticket-incident} (ADR-0007). */
	@Override
	protected boolean statusFollowsALifecycleProcess() {
		return true;
	}
}
