package dev.alkolhar.servdesk.ticket;

import dev.alkolhar.servdesk.common.exception.ConflictException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowNode;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.common.engine.api.FlowableObjectNotFoundException;
import org.flowable.common.engine.api.FlowableOptimisticLockingException;
import org.flowable.common.engine.impl.identity.Authentication;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Component;

/**
 * A ticket's open lifecycle tasks, in the API's terms (ADR-0008): a stage's
 * task key and the actions it offers — never a Flowable task id outside this
 * class.
 * <p>
 * <b>The action catalogue is read from the BPMN</b>, not kept in Java: the
 * exclusive gateway after a stage's user task has one outgoing sequence flow
 * per outcome, <i>named</i> after it (the convention every
 * {@code processes/*.bpmn20.xml} follows). So a new process, or a changed one,
 * brings its actions with it, and a ticket on an older process version is
 * offered exactly what its version can do.
 */
@Component
public class TicketTasks {

	/**
	 * One open task. {@code assigneeId} is the task's assignee as a Person id
	 * (ADR-0009); nobody assigns tasks yet (#113), so it's null for now.
	 */
	public record OpenTask(String taskId, String key, @Nullable Long assigneeId, Instant createdAt,
			List<String> actions) {
	}

	private final TaskService taskService;
	private final RepositoryService repositoryService;

	public TicketTasks(TaskService taskService, RepositoryService repositoryService) {
		this.taskService = taskService;
		this.repositoryService = repositoryService;
	}

	/**
	 * Empty for a ticket without a running process: a closed one, or a subtype not
	 * yet on its lifecycle process. One entry in the MVP; a list so parallel tasks
	 * fit later without changing callers.
	 */
	public List<OpenTask> openTasks(Long ticketId) {
		return taskService.createTaskQuery().processInstanceBusinessKey(String.valueOf(ticketId))
				.orderByTaskCreateTime().asc().list().stream()
				.map(task -> new OpenTask(task.getId(), task.getTaskDefinitionKey(), assigneeIdOf(task),
						task.getCreateTime().toInstant(), actionsOf(task)))
				.toList();
	}

	/**
	 * Completes {@code task} with {@code outcome}, recording {@code callerId} (a
	 * Person id) as Flowable's authenticated user, so the engine's own history says
	 * who did it in the same terms (ADR-0009). Joins the caller's transaction. A
	 * task someone else completed a moment earlier is a 409, not a 500: the ticket
	 * moved on, and the caller should look again.
	 */
	public void complete(OpenTask task, String outcome, Long callerId) {
		String userId = String.valueOf(callerId);
		// both: the authenticated user is what the engine stamps on what it records
		// by itself; the task's completedBy comes only from this userId argument
		Authentication.setAuthenticatedUserId(userId);
		try {
			taskService.complete(task.taskId(), userId, Map.of("outcome", outcome));
		} catch (FlowableObjectNotFoundException | FlowableOptimisticLockingException e) {
			throw new ConflictException("The ticket moved on while this action was being performed; reload it");
		} finally {
			Authentication.setAuthenticatedUserId(null);
		}
	}

	private List<String> actionsOf(Task task) {
		// Flowable caches the parsed model per process definition version
		BpmnModel model = repositoryService.getBpmnModel(task.getProcessDefinitionId());
		if (!(model.getFlowElement(task.getTaskDefinitionKey()) instanceof FlowNode stage)) {
			return List.of();
		}
		return stage.getOutgoingFlows().stream().map(flow -> model.getFlowElement(flow.getTargetRef()))
				.filter(FlowNode.class::isInstance).map(FlowNode.class::cast)
				.flatMap(gateway -> gateway.getOutgoingFlows().stream()).map(SequenceFlow::getName)
				.filter(name -> name != null && !name.isBlank()).distinct().toList();
	}

	private static @Nullable Long assigneeIdOf(Task task) {
		String assignee = task.getAssignee();
		return assignee == null ? null : Long.valueOf(assignee);
	}

}
