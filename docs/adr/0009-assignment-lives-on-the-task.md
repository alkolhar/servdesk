---
status: accepted
---

# Assignment lives on the task; the ticket mirrors it

ADR-0008 made every lifecycle stage a BPMN user task and made the ticket listing the MVP inbox. That
only works if a ticket's Assignee and Team always match its current task. This ADR decides **how
tasks are assigned, who may do what, and how the ticket stays in step**. It completes the
projection pattern of ADR-0004 (status) and ADR-0008 (tasks): **the task is the source of truth for
assignment, and `Ticket.assignee`/`Ticket.team` mirror it.**

## Considered options

- **Username and team name as Flowable's assignee and candidate group.** Rejected: usernames change
  through `PUT /api/persons` (and not every person has one), and team names change. Either would
  silently orphan tasks.
- **Assignment kept as ordinary `PUT` fields**, passed on to the task behind the scenes. Rejected:
  a field edit would quietly perform a task operation, the pattern already removed for `status`,
  `password` and `enabled`.
- **Assignment as ticket actions** (`POST …/actions/assign`). Rejected: ADR-0008 defines actions as
  task outcomes that move the ticket on, and assigning doesn't.
- **Blocking deactivation until a person's tickets are reassigned.** Rejected: it turns an urgent
  offboarding into a chore, and invites admins mass-assigning work to themselves just to get past
  the guard.
- **The Change approval as a second, parallel task** next to the planner's own. Deferred: it's the
  natural shape once Flowable is used more natively, but the MVP keeps one open task per ticket
  (ADR-0008).

## Decision

- **Identifiers.** A task's `assignee` is the **Person id**, and its candidate group is the
  **Team id** (Flowable stores both as strings). No candidate users in the MVP. On every operation,
  the caller's Person id is set as Flowable's authenticated user, so the engine's own history
  records who did what in the same terms.
- **One writer.** A single assignment service changes a task's assignee or candidate team and, **in
  the same transaction**, writes `Ticket.assignee`/`Ticket.team` to match. Nothing else writes those
  columns.
- **Operations**, each exposed as an affordance link (ADR-0003) only to callers allowed to use it:
  - `PUT /api/tickets/{id}/assignment` with `{assigneeId, teamId}` sets the current task's assignee
    and/or candidate team. Null means none: this endpoint is only about assignment, so an omitted
    field can't be an accident.
  - **`claim`**: assign an unassigned task to yourself.
  - **`release`**: unassign yourself; the task returns to its team's queue.
  - `assigneeId`/`teamId` **leave the ticket `PUT`** and stay on create as the initial routing for
    the first task.
- **Who may do what:**

  | operation | allowed for |
  |---|---|
  | act (ADR-0008 actions) | the assignee; on an unassigned task, a member of its team, and acting claims it; with no team either, any Agent |
  | claim | an unassigned task, by its team's members (any Agent if it has no team) |
  | release | the assignee |
  | assignment | any Agent (routing is everyday work; the Triage stage exists for it) |
  | all of the above | admins, as an override |

  Nobody grabs a task someone else holds; taking over goes through `assignment` or the holder's
  `release`. An assignee must be an **active, login-capable Agent**, and, if the task has a team,
  **a member of that team**. Handing work across teams means changing the team.
- **Between stages** the next task **inherits the assignee and team**, so the agent who started
  work keeps the ticket through hold, resume, resolve and reopen.
- **Change approval.** `approve-change` is offered, **unassigned**, to a **designated approver
  team**, configured per deployment (`servdesk.change.approver-team-id`), with **admins** as the
  fallback when none is configured. During approval the ticket mirrors that task, so the Change
  sits in the approvers' queue. The previous assignee and team are kept in a process variable and
  **restored** on the `work` task after `approve`. No segregation-of-duties check in the MVP.
- **When people and teams change:**
  - **Deactivating a person releases all their tasks**, in the same transaction, back to each
    task's team queue. A task with no team becomes unassigned and open to every Agent.
  - **Removing a person from a team releases their tasks in that team.**
  - **Reactivation restores nothing**, since the work was re-queued and may have been picked up.
  - A team can't be deleted while tickets reference it (#89), which covers its open tasks.

## Direction

The maintainer's stated direction: **later, BPMN lanes decide assignment.** Flowable treats lanes
as purely visual, so each lane's tasks declare their candidate team. That's the same "assignment
lives on the task" model this ADR sets up. Parallel tasks, such as a planner plus an approver, come
with more native Flowable use (ADR-0008).

## Consequences

- The contract gains `/api/tickets/{id}/assignment` and the `assign`/`claim`/`release` link
  relations; `assigneeId`/`teamId` leave the ticket update requests (a breaking change, like
  `status`).
- The inbox (ADR-0008) works off columns that can't drift: every change goes through the one
  assignment service.
- Deactivation and team-membership changes (#89) now also touch tasks, so the person service calls
  the assignment service as part of those operations.
- Unassigned tickets with no team need to be visible somewhere, as a queue for all Agents; that's a
  requirement on the screen list (#97).
- `CLAUDE.md`'s ticketing section must describe assignment as task-owned when this is built.
