package dev.alkolhar.servdesk.ticket;

import org.flowable.engine.RuntimeService;
import org.springframework.stereotype.Component;

/**
 * Starts and ends a ticket's lifecycle process instance (ADR-0004, ADR-0007):
 * exactly one per ticket, business key = the shared {@link Ticket} id, in the
 * caller's transaction — Flowable joins Spring's, so if the start fails the
 * ticket isn't created, and a ticket never exists without its process.
 */
@Component
public class TicketProcesses {

	private final RuntimeService runtimeService;

	public TicketProcesses(RuntimeService runtimeService) {
		this.runtimeService = runtimeService;
	}

	/** Starts {@code processKey} for a ticket that has just been inserted. */
	public void start(String processKey, Ticket ticket) {
		runtimeService.startProcessInstanceByKey(processKey, businessKey(ticket));
	}

	/**
	 * Ends the ticket's running instance, if it has one — a closed ticket's process
	 * has already finished, so there's nothing left to end.
	 */
	public void end(String processKey, Ticket ticket, String reason) {
		runtimeService.createProcessInstanceQuery().processDefinitionKey(processKey)
				.processInstanceBusinessKey(businessKey(ticket)).list()
				.forEach(instance -> runtimeService.deleteProcessInstance(instance.getId(), reason));
	}

	private static String businessKey(Ticket ticket) {
		return String.valueOf(ticket.getId());
	}

}
