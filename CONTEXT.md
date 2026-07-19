# Servdesk

An ITSM ticketing system: agents track and resolve tickets raised by customers.

## Language

**Ticket**:
The shared case record every ticket subtype ([[Incident]], [[Problem]], [[Change]], [[Service Request]]) composes with via a shared id — holds the fields common to all of them: status, category, priority, team, assignee, requester, timestamps. Never exists standalone; always paired with exactly one subtype. See [ADR-0001](./docs/adr/0001-ticket-subtypes-composed-not-inherited.md).
_Avoid_: Case

**Incident**:
An unplanned interruption or reduction in quality of a service, raised so an Agent can restore it. May link to the Problem tracking its root cause.

**Problem**:
The underlying root cause behind one or more Incidents. Problem Management is the practice of tracking down and fixing root causes so the related Incidents stop recurring.

**Change**:
A planned modification to infrastructure or a service.

**Service Request**:
A routine request for something (e.g. a new account, access to a system), not a report of something broken.
_Avoid_: Request

**Agent**:
A servdesk staff member who can be assigned tickets, change ticket status, and manage the person directory. Persisted as a `Person` with role `AGENT`.
_Avoid_: Person, staff, technician

**Customer**:
A person who raises tickets and receives support, but cannot change ticket status or manage the person directory. Persisted as a `Person` with role `CUSTOMER`.
_Avoid_: Person, client, user

**Requester**:
The person a ticket is raised on behalf of. Usually a Customer, but can be an Agent for an internal-facing ticket (e.g. the agent's own equipment). A role a Person plays on a given ticket, not a synonym for Customer.
_Avoid_: Customer (when specifically referring to the ticket's requester field), reporter, submitter

**Assignee**:
The Agent who owns resolving a ticket. Always an Agent, never a Customer — a ticket waiting on the requester is expressed via status ([[Pending]]), not by assignment.
_Avoid_: Owner, handler

**Team**:
A group of Agents a ticket is routed to as a queue, before or instead of being picked up by an individual Assignee. An Assignee is normally a member of the ticket's team, but this is a soft convention, not an enforced constraint — cross-team assist (an agent from another team picking up a ticket) is a valid scenario.
_Avoid_: Queue (describes the role a Team plays on a ticket, not a separate concept), group, department

**Category**:
A hierarchical classification a ticket can be tagged with (e.g. "Hardware → Laptop → Battery"), used for filtering and reporting only. Purely descriptive — it does not drive routing or any other behavior today.
_Avoid_: Tag, type (a ticket's type is which subtype it is — [[Incident]]/[[Problem]]/[[Change]]/[[Service Request]] — not a Category value)

**Priority**:
A flat ranking (`sortOrder`, lower = more severe) a ticket can be given. No longer directly selected — server-derived from a ticket's [[Impact]] and [[Urgency]] pair via [[Priority Definition]] (classic ITIL priority matrix), the same "never client-supplied" treatment as [[Resolved]]/[[Closed]]'s timestamps. Stays unset if either input is missing, or if the pair has no matching Priority Definition — a gap in the matrix is a data-quality concern for whoever maintains it, not a reason to reject the ticket write.
_Avoid_: Severity (not yet a distinct concept in this domain)

**Impact**:
One axis of the priority matrix — how broadly a ticket's underlying issue affects the business (e.g. one person vs. a whole department). A flat, admin-configurable lookup (`sortOrder`, lower = more severe), same shape as [[Priority]] itself.

**Urgency**:
The other axis of the priority matrix — how quickly a ticket's underlying issue needs addressing, independent of how broadly it's felt. Same flat, admin-configurable shape as [[Impact]].

**Priority Definition**:
One cell of the priority matrix: maps a single ([[Impact]], [[Urgency]]) pair to the [[Priority]] a ticket carrying that pair should be given. Unique per (Impact, Urgency) pair, but multiple pairs may resolve to the same Priority (e.g. both Low/Low and Medium/Low could both mean "Low priority").
_Avoid_: Priority matrix (describes the whole mapping table this entity is one row of, not a separate concept)

**Comment**:
An entry in a ticket's activity/conversation history, written by an Agent or Requester. The `internal` flag distinguishes an Agent-only note from a reply visible to the Requester — one concept with a visibility attribute, not two different kinds of thing. Only an Agent can author an internal Comment — a Customer's own comment is always visible, since "internal" means "hidden from the requester," which is meaningless for a comment the requester wrote themselves. Enforced in code: `CommentCommandService.create` rejects `internal=true` from a non-Agent caller, and `CommentQueryService.findByTicket` filters internal comments out of a Customer's view.
_Avoid_: Note, Work Note, Reply (as separate concepts — these describe the Comment's visibility, not a different kind of entity)

## Ticket Status

**Open**:
The ticket has been raised and no agent has started work on it yet.

**In Progress**:
An agent is actively working the ticket.

**Pending**:
The ticket is blocked, waiting on someone outside the agent's control to respond or act — the requester, a vendor, an approval, etc. A broader concept than "waiting on customer"; narrower, more detailed statuses may be split out later once a workflow/process engine exists.

**Resolved**:
The agent believes the underlying issue is fixed, but the ticket stays open for the requester to confirm or reopen. `resolvedAt` is set by the server the moment status transitions to Resolved — never client-supplied.

**Closed**:
Terminal — no further action is expected, whether reached by requester confirmation or an automatic timeout. `closedAt` is set by the server the moment status transitions to Closed — never client-supplied.
