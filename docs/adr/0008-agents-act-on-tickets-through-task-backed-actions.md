---
status: accepted
---

# Agents act on tickets through task-backed actions

ADR-0004 made the process authoritative for a ticket's status and made `status` read-only on
`PUT`, and ADR-0007 shaped one lifecycle process per subtype. This ADR decides **how Agents move a
ticket along**: every stage holds a BPMN **user task**, and the API exposes that task's outcomes
as **ticket actions**. The engine does the work; the contract speaks tickets.

A standing requirement shapes it: **work is done in BPMN user tasks with real assignees**, not only
approvals.

## Considered options

- **Expose raw Flowable tasks** (`GET /api/tasks`, `POST /api/tasks/{id}/complete` with an outcome
  variable). Rejected: the contract would speak the engine's vocabulary (task ids, outcome
  variables), and "resolve this ticket" would become "complete task 8f3a… with outcome=resolve".
- **Named transitions implemented as BPMN messages/signals, with user tasks only for real
  decisions** (approval). Rejected: everyday work would have no task, so assignment would live
  only on `Ticket.assignee`, and the requirement above would hold only for approvals.
- **A task-based inbox** (a listing API over Flowable's task tables). Rejected **for the MVP**: a
  second listing next to `/api/tickets`, where filters on priority, category, SLA and custom fields
  would join the engine's task query with servdesk's tables, with paging across two data
  sources. See "Direction" below.
- **Only action links on the ticket**, with no view of the task itself. Rejected: the UI couldn't
  show what a ticket is waiting on, and there would be nothing to build a native task view on
  later. A separate `/api/tickets/{id}/tasks` resource was rejected as an extra call on every
  ticket screen.

## Decision

- **Every stage holds one user task**, whose outcomes are that stage's transitions. The basic flow,
  shared by all four processes (ADR-0007):

  | status | task key | actions |
  |---|---|---|
  | Open | `triage` | `start-work` → In Progress · `cancel` → Closed |
  | In Progress | `work` | `resolve` → Resolved · `put-on-hold` → Pending · `cancel` → Closed |
  | Pending | `on-hold` | `resume` → In Progress · `cancel` → Closed |
  | Resolved | `confirm-resolution` | `close` → Closed · `reopen` → In Progress · *(timer after 5 days → Closed)* |

  The Change process replaces `start-work` at Open with `submit-for-approval`, which leads to
  `approve-change` (projecting to Pending) with `approve` → In Progress and `reject` → Closed.
- **`cancel`** closes a ticket that shouldn't be worked (duplicate, spam, raised by mistake)
  without pretending it was fixed: `closedAt` is set and `resolvedAt` stays empty, so resolution
  metrics stay honest.
- **One operation:** `POST /api/tickets/{id}/actions/{action}` completes the ticket's current task
  with that outcome. Its body may carry `{ "comment": "…" }`, stored as a normal (non-internal)
  comment on the ticket. The comment is **required** for `resolve` (the resolution note), `cancel`
  and `reject`. An action that isn't available in the current stage, or not to this caller, is
  refused. Flowable task ids never appear in the contract.
- **Action links.** The ticket model carries an `action:<name>` link for each outcome this caller
  may use now, and no others (ADR-0003). Who may act on a task (assignee, candidate group,
  claiming) is decided with task assignment (#87); the links follow that rule.
- **The ticket model gets a `tasks` array.** Each entry holds `key` (stable, and translated by the
  UI), `assigneeId`, `teamId` (candidate group), `createdAt`, and its own action links. In the MVP
  it holds exactly one entry. It is an array on purpose: parallel tasks fit later without changing
  the contract. The task `key` also covers the planned `stage` projection (ADR-0007).
- **The inbox is the ticket listing** for the MVP: "my work" is `GET /api/tickets?assignee=…` and
  a team's queue is `?team=…` restricted to unassigned tickets, with the existing paging, sorting
  and filters. This relies on **`Ticket.assignee` and `Ticket.team` mirroring the current task's
  assignee and candidate group**, written in one place, in the same transaction. That is a
  requirement on task assignment (#87), the same projection pattern as `status` (ADR-0004).

## Direction

The maintainer's stated direction: **after the MVP, Flowable should be used as natively as
possible**, including a real task inbox, several tasks open in parallel, delegation and task
forms. The MVP must not rule that out. The `tasks` array, per-task action links and task keys
are the bridge; nothing in the API should assume more than one open task beyond what the MVP
strictly needs. A task-based listing can then be added next to the ticket listing.

## Consequences

- The contract gains the actions endpoint, the `action:*` link relations and the `tasks` array on
  every ticket model. `PUT` keeps editing descriptive fields only.
- Listing queries and filters stay SQL over `ticket`; the inbox needs at most small extensions
  (such as an unassigned filter), not a new API.
- The Assignee, and the Team a ticket shows under, become projections of the current task, as
  status already is. Keeping them in sync is the job of #87's design.
- Testing an action means driving a real process engine; the action catalogue belongs in the shared
  per-subtype process test from ADR-0007.
- The UI can show "what is this ticket waiting on, for whom, since when" from `tasks` without a
  second call.
