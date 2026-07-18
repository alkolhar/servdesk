---
status: accepted
---

# Ticket subtypes compose with a shared Ticket record, not JPA inheritance

Incident/Problem/Change/Service Request are each expected to accumulate their own type-specific
fields over time (e.g. an Incident→Problem root-cause link, a future Change approval workflow) that
don't fit cleanly on one flat `Ticket` entity — superseding the single-entity design originally
recorded for `Ticket` in `CLAUDE.md`. Each subtype is a standalone `@Entity` holding a
`@OneToOne @MapsId` reference to a shared, concrete `Ticket` record (id, requester, status, category,
priority, team, assignee, timestamps), rather than a JPA class-inheritance hierarchy rooted in an
abstract `Ticket` entity.

## Considered options

- **`SINGLE_TABLE` inheritance** — rejected: every subtype's fields end up nullable on one
  ever-growing table, with no DB-level `NOT NULL` enforcement per subtype.
- **`JOINED` inheritance** — rejected: the main justification (avoiding a join when listing tickets
  across all types) turned out not to be a real requirement — this domain never lists tickets across
  types; an ITSM tool lists Incidents, Changes, etc. separately, each with different columns. Without
  that requirement, Hibernate's polymorphic entity/proxy machinery is complexity this design doesn't
  need.
- **Polymorphic FK on `TicketComment`** (a `(ticketId, ticketType)` pair with no real foreign key,
  à la Rails' polymorphic associations) — rejected: gives up DB-level referential integrity, which
  this project has consistently favored elsewhere (`ddl-auto=validate` catching mapping drift, real
  unique constraints).

## Consequences

- `TicketComment.ticket` references the shared, concrete `Ticket` entity directly — a real FK, no
  polymorphism required.
- Each subtype gets its own repository/command/query services, sharing a common base for the CRUD
  that operates on the shared `Ticket` fields.
- `ticketNumber` moves off the shared `Ticket` entity onto each subtype (its own prefix and its own
  DB sequence, e.g. `INC-`/`incident_number_seq`, `RFC-`/`change_number_seq`), since human-facing
  ticket numbers are type-specific, not uniform.
- There is no cross-type "list all tickets" query in this design — listing is inherently per subtype.
