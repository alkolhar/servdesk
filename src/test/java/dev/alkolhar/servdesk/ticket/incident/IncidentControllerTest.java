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
}
