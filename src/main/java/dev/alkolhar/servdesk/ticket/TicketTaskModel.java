package dev.alkolhar.servdesk.ticket;

import java.time.Instant;
import org.jspecify.annotations.Nullable;
import org.springframework.hateoas.RepresentationModel;

/**
 * One entry of a ticket model's {@code tasks} array (ADR-0008): what the ticket
 * is waiting on, for whom, since when — and, as {@code action:*} links, what
 * this caller may do about it now. {@code key} is the stage's stable task key
 * (the UI translates it); Flowable's task id never appears.
 */
@SuppressWarnings("NotNullFieldNotInitialized")
public class TicketTaskModel extends RepresentationModel<TicketTaskModel> {

	private String key;
	private @Nullable Long assigneeId;
	private @Nullable Long teamId;
	private Instant createdAt;

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public @Nullable Long getAssigneeId() {
		return assigneeId;
	}

	public void setAssigneeId(@Nullable Long assigneeId) {
		this.assigneeId = assigneeId;
	}

	/**
	 * The task's candidate team (ADR-0009); filled once tasks are assigned (#113).
	 */
	public @Nullable Long getTeamId() {
		return teamId;
	}

	public void setTeamId(@Nullable Long teamId) {
		this.teamId = teamId;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}

}
