package dev.alkolhar.servdesk.ticket.servicerequest;

import dev.alkolhar.servdesk.ticket.AbstractTicketSubtypeControllerTest;

class ServiceRequestControllerTest extends AbstractTicketSubtypeControllerTest {

	@Override
	protected String basePath() {
		return "/api/service-requests";
	}

	@Override
	protected String expectedDisplayNumberPrefix() {
		return "REQ-";
	}
}
