---
status: accepted
---

# Flowable drives the ticket lifecycle; `Ticket.status` is its projection

Each ticket's lifecycle is driven by a **Flowable BPMN process instance** (Flowable 8, embedded; see
`docs/research/2026-09-29-flowable-on-spring-boot-4.md`). The **process instance is authoritative**
for where a ticket is. `Ticket.status` stays a real, queryable column, but it is a **projection**
written only when the process enters a lifecycle stage. Clients no longer set a status; they act on
the process, and the status follows.

Until now, `status` was a plain field set from the `PUT` body. Its side effects (`resolvedAt`/
`closedAt`, the SLA pause, `TicketStatusChangedEvent`) were derived by comparing `TicketStatus`
enum ordinals, and any status was reachable from any other (#46).

## Considered options

- **The column is authoritative, and the process checks and follows**: `PUT status` stays, the
  service asks the process whether the transition is allowed, then moves it along. Rejected: two
  sources of state kept in sync in both directions, so every divergence is a bug, and Flowable
  would gate the lifecycle rather than drive it.
- **The process only**: status leaves the `ticket` table, and reads go through Flowable's query API.
  Rejected: paging, the `?status=` filters and the SLA breach scanner are SQL over `ticket`.
  Flowable's run state lives in its own `ACT_RU_*` tables, and joining those into every listing
  isn't realistic.
- **Side effects modelled in BPMN** (timestamps by script tasks, SLA pause by process listeners), or
  split between BPMN and Java. Rejected: business logic in BPMN XML is hard to test, and SLA
  already has its home in `TicketSlaService`. A split means two places to look for the same kind of
  rule.
- **Starting the process asynchronously after the ticket commits.** Rejected: tickets could exist,
  briefly or permanently, with no process, and every read would have to handle that.

## Decision

- **The process decides *when*; Java decides *what it means*.** A single entry point, for example
  `TicketLifecycle.enterStatus(ticketId, newStatus)`, is the **only code that writes
  `Ticket.status`**. The BPMN calls it (execution listener or delegate) on entering each stage, and
  it runs in that process step's transaction. It applies the side effects: `resolvedAt`/`closedAt`,
  the SLA pause and resume via `SlaHooks` (so `ticket` still never imports `sla`), and
  `TicketStatusChangedEvent`. The side effects compare **explicit previous and new statuses**; no
  enum ordinals.
- **`status` is read-only on `PUT`** for every ticket subtype. How agents act on a lifecycle (task
  completion, transition operations, the task inbox) is decided separately (#86). Whatever the
  process allows reaches the UI as **action links** (ADR-0003).
- **Exactly one process instance per ticket**, started in the **same transaction** as the ticket's
  creation, with **business key = the shared `Ticket` id** (stable across subtypes, and the id
  comments already hang off). The initial status is set by the process too; the column's `OPEN`
  default stays only as a fallback in the schema. If the process start fails, creating the ticket
  fails.
- **Soft-deleting a ticket deletes its process instance**, with a reason, in the same transaction.

This relies on Flowable's Spring integration joining the context's `PlatformTransactionManager`:
Boot's `JpaTransactionManager` shares its connection with Flowable, so a JPA write and a
synchronous process step commit or roll back together (verified in the research above).
Asynchronous continuations and timer jobs run in their own transactions; each still writes
`status` only through the same entry point, inside that job's transaction.

## Consequences

- Listings, filters, the SLA scanner, `GET /api/tickets` and the contract's `status` field keep
  reading the column unchanged.
- **Breaking API change:** `status` leaves the ticket update requests. Nothing outside CI uses the
  API yet.
- **Which transitions are allowed is whatever the process models.** A static transition map (#46)
  is no longer needed; #46 is closed as superseded.
- The process definitions, how they map to the four subtypes, whether `TicketStatus` survives as it
  is, and what happens after a process ends (reopening) are decided separately (#85). Flowable's
  schema and Flyway, likewise (#84).
- Tests of the side effects stay plain unit tests of the entry point, with mocked collaborators. Tests
  of *transitions* now need a running process engine.
