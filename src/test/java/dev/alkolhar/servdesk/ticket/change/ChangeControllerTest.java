package dev.alkolhar.servdesk.ticket.change;

import dev.alkolhar.servdesk.ticket.AbstractTicketSubtypeControllerTest;

class ChangeControllerTest extends AbstractTicketSubtypeControllerTest {

	@Override
	protected String basePath() {
		return "/api/changes";
	}

	@Override
	protected String expectedDisplayNumberPrefix() {
		return "RFC-";
	}
}
