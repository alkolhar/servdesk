package dev.alkolhar.servdesk.ticket.problem;

import dev.alkolhar.servdesk.ticket.AbstractTicketSubtypeControllerTest;

class ProblemControllerTest extends AbstractTicketSubtypeControllerTest {

	@Override
	protected String basePath() {
		return "/api/problems";
	}

	@Override
	protected String expectedDisplayNumberPrefix() {
		return "PRB-";
	}
}
