---
status: accepted
---

# Flowable owns its own tables; Flyway owns servdesk's

The rule so far was absolute: **Flyway migrations are the only source of schema truth**, and
Hibernate only validates (`ddl-auto=validate`). Embedding Flowable 8 (ADR-0004) brings a second
schema owner: its BPMN engine ships its own PostgreSQL DDL and a chain of per-version upgrade
scripts, and by default its Spring Boot starter creates and upgrades those tables at startup
(`flowable.database-schema-update=true`). Facts: `docs/research/2026-09-29-flowable-on-spring-boot-4.md` §3.

This ADR narrows the rule to what servdesk owns: **Flyway owns servdesk's tables; Flowable owns
its `act_*`/`flw_*` tables; neither touches the other's.**

## Considered options

- **Flyway owns Flowable's tables too.** Copy Flowable's four PostgreSQL create scripts (common,
  engine, history, event registry) into a migration, run Flowable with
  `database-schema-update=false` (a strict version check), and on every Flowable upgrade work out
  which of its upgrade step scripts apply, then add them as a new migration in the right order.
  Rejected: it hand-maintains exactly the part upstream already tests (85 upgrade steps in
  `flowable-engine` alone), and a missing or mis-ordered step within the right version range
  would pass the version check. It adds work and a new way to fail, without adding safety.
- **Flowable's tables in a separate Postgres schema.** The ownership split would be visible in
  the database itself, but Flowable's SQL isn't schema-qualified by default: it needs a table
  prefix or `search_path` handling that nobody has verified on Flowable 8 and Postgres. Rejected
  for the MVP; revisit if the shared schema ever causes real trouble.

## Decision

- **Flowable manages its own tables**, with `flowable.database-schema-update=true`, in the same
  schema as servdesk's. Its upgrader runs at startup, as Flyway's migrations do, so upgrades
  happen at the same moment either way.
- **The boundary is strict, in both directions.** servdesk's Flyway migrations never create,
  alter or reference `act_*`/`flw_*` tables, and there are **no foreign keys either way**. The
  link between a ticket and its process instance is the process's business key, the shared
  `Ticket` id (ADR-0004), not a column. Hibernate's `ddl-auto=validate` is unaffected, because
  Flowable doesn't go through Hibernate.
- **Only the engine's own tables.** The IDM engine stays off (`flowable.idm.enabled=false`), so no
  `act_id_*` tables; identity stays with `Person` and Spring Security.
  `flowable.use-lock-for-database-schema-update` stays off (one instance per customer).
- **PostgreSQL 17, pinned** everywhere (compose, CI, Testcontainers) to an exact version plus
  digest. Flowable 8's CI covers Postgres 14–17, and ADR-0002 makes the database part of the
  product, so servdesk runs the newest major its embedded engine is tested against. Moving to 18
  is deliberate, once Flowable covers it.
- **History level `audit`** (Flowable's default): process instances, activities, tasks with
  assignee and completion, final variable values. **Flowable's history is for operations and
  debugging, not the product's audit trail.** A ticket activity timeline (#47) is built on
  servdesk's own data, so the UI never reads engine tables. Retention of `act_hi_*` belongs with
  data retention (#51).

## Consequences

- `CLAUDE.md`'s "Flyway is the only schema source of truth" wording must name this exception when
  Flowable is added.
- A Flowable upgrade is a dependency bump; its schema changes arrive with it. Reading its release
  notes and upgrade scripts is part of reviewing that bump, like any other dependency change.
- Flowable's tables are created at application startup, so an empty database is fine and there's
  no extra setup step for deployments or Testcontainers.
- `postgres:latest` goes away in compose, CI and `TestcontainersConfiguration`.
- History grows with every ticket until retention (#51) enables Flowable's history cleanup.
