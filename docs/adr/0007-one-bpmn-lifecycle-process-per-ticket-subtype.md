---
status: accepted
---

# One BPMN lifecycle process per ticket subtype

ADR-0004 made a Flowable process instance authoritative for where a ticket is, with
`Ticket.status` as its projection. This ADR decides **which processes exist and how they are
shaped**: one BPMN process definition per ticket subtype, all projecting onto the same five
statuses.

## Considered options

- **One shared process for all four subtypes**, branching on the subtype where they differ.
  Rejected: every subtype-specific rule becomes a conditional in one diagram that gets harder to
  read with each difference.
- **A shared core lifecycle as a reusable subprocess**, called from thin per-subtype processes
  (call activities). Rejected: a call activity starts a *child* process instance, which blurs
  ADR-0004's "exactly one process instance per ticket", and task queries would have to look in
  both.
- **Per-process status vocabularies** (e.g. `AWAITING_APPROVAL` as a real status of Changes).
  Rejected: cross-subtype listings, the `?status=` filter and SLA pause/resume rely on one shared
  meaning. The column would become a free string, and SLA would need per-process rules.
- **A rejected Change looping back to Open for rework.** Rejected: a ticket's history would hold
  several approval rounds, and "rejected" would not be a stable outcome.
- **Migrating every running instance on every process change.** Rejected as the default: most
  process changes are fine for new tickets only, and a migration plan per change would slow down
  every change to a process.

## Decision

- **The five statuses stay the shared projection.** `Open`, `In Progress`, `Pending`, `Resolved`,
  `Closed` keep their meanings for every subtype. A process may have richer stages inside, but
  **each stage maps to exactly one status**. A separate `stage` projection (the current named
  step, for display and filtering within a subtype) is a **planned, additive extension**.
- **One process definition per subtype:** `ticket-incident`, `ticket-problem`, `ticket-change`,
  `ticket-service-request`, shipped as `src/main/resources/processes/*.bpmn20.xml` and deployed
  by Flowable at startup. Each starts from the same basic flow (Open → In Progress ⇄ Pending →
  Resolved → Closed) and diverges only where the practice does. A **shared test** checks all four
  against that common flow, as `AbstractTicketSubtypeControllerTest` does for the controllers.
  The subtype chooses its process key when the ticket is created.
- **Change approval is a user task** between Open and In Progress:

  ```
  Open ──submit for approval──▶ [Awaiting approval] ──approve──▶ In Progress ──▶ Resolved ──▶ Closed
                                       │
                                       └──reject (reason required)──▶ Closed
  ```

  "Awaiting approval" projects to `Pending`, so the SLA clock pauses (`CONTEXT.md` already counts
  an approval as a reason for Pending). **Rejection is final:** it closes the Change with a
  required reason recorded as a comment, and a new attempt is a new Change. Approval can't be
  bypassed, because the process is the only path to `In Progress`. Who the task is offered to is
  decided with task assignment (#87). This supersedes the hand-built approval in #23.
- **Resolved → Closed.** In `Resolved`, an Agent can **close** the ticket or **reopen** it (back to
  `In Progress`, `resolvedAt` cleared). A **BPMN timer auto-closes** a resolved ticket after a
  configurable period, **5 days by default** (a configuration property, not a value in the BPMN),
  standing in for requester confirmation until a customer portal exists. **Flowable's async job
  executor is therefore on**; the timer runs in its own transaction and writes status through the
  single entry point (ADR-0004).
- **Closed is terminal.** The process ends; a closed ticket is not reopened. A recurrence is a new
  ticket, which can reference the earlier one. A rejected Change ends the same way.
- **Versioning.** A changed BPMN file becomes a new version of its definition. **Running tickets
  stay on the version they started with**, and a release ships an explicit Flowable process-instance
  migration only when its change must apply to tickets already in flight. The rule that keeps the
  mix safe: **every version's stages map to the same five statuses**, so listings and SLA never
  notice which version a ticket runs on.

## Consequences

- The operations that move a ticket (start work, put on hold, resume, resolve, close, reopen, submit
  for approval, approve/reject) and whether they are user tasks or transition operations are
  decided in #86; they reach the UI as action links (ADR-0003).
- The four per-subtype processes duplicate the basic flow. The shared test is what keeps them from
  drifting apart unintentionally.
- For a while after a release, tickets may run on older process versions. That's safe as long as
  the projection rule holds, and an explicit migration is available when it isn't enough.
- #23 is closed as superseded. Its out-of-scope items (segregation of duties, approval depending
  on risk) remain possible future changes to the Change process.
- The auto-close timer is the first scheduled work inside the engine; SLA breach scanning stays on
  Quartz for now (whether SLA timing moves into BPMN is still open on the map).
