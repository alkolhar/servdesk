---
status: accepted
---

# PostgreSQL only; the database ships as part of the product

servdesk serves multiple customers as **one standard product deployed per customer, customized
only through configuration** — per-customer code or builds are explicitly off the table, and so is
a shared multi-tenant instance. That model makes customer-defined custom fields (on tickets, and
on CMDB configuration items once those exist) the product's core customization mechanism, and the
viable design for those — a `jsonb` document column with a GIN index, validated against
admin-editable attribute definitions — is PostgreSQL-specific in practice. servdesk therefore
targets **PostgreSQL exclusively**, and the database is part of the deliverable (shipped alongside
the app via the compose file / container images), never an engine the customer picks.

## Considered options

- **Stay on MariaDB** (the original engine) — rejected. Its JSON support is a `LONGTEXT` alias;
  indexing a JSON path requires a generated column per field, which cannot work for fields
  customers define at runtime — MariaDB would have forced the EAV-table design instead. It also
  has no partial unique indexes, which had already produced a documented standing defect: a
  soft-deleted row's unique values (email, username, team/priority name, display numbers) kept
  occupying their index, so recreating one surfaced as a spurious 409.
- **Database portability** (customer-chosen engine, lowest-common-denominator SQL) — rejected.
  It trades the features above for a compatibility burden a solo-maintained product can't carry,
  and the modern self-hosted posture (GitLab, Sentry, Keycloak) is precisely "the product owns its
  database." Nothing was deployed yet, so the clean cut was free.

## Consequences

- Migrations and queries may use Postgres-specific features freely: partial unique indexes
  (`WHERE deleted_at IS NULL`, replacing the soft-delete 409 trade-off), `jsonb` + GIN for the
  custom-field foundation, and later full-text search (`tsvector`) and `SKIP LOCKED` queues —
  no external search engine or broker needed for v1 of those features.
- Unbounded text columns are plain `TEXT`; entity fields use `@JdbcTypeCode(SqlTypes.LONGVARCHAR)`
  rather than `@Lob`, which on PostgreSQL would map to `oid` large objects.
- The `change_request` table keeps its name (a MariaDB reserved-word workaround originally; the
  clearer name regardless).
- A future switch of engines would be a real migration project — accepted deliberately.
