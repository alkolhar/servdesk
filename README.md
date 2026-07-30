# servdesk

An ITSM (IT service management) ticketing application: a hypermedia REST API built as a modular
monolith on Java 25 and Spring Boot 4.1, with PostgreSQL as part of the product rather than a
swappable dependency.

The domain model, REST layer, security, CI, and OpenAPI contract testing are in place. Feature work
is tracked in [GitHub Issues](https://github.com/alkolhar/servdesk/issues), not in this file.

## What's built

- **Tickets** — four subtypes (Incident, Problem, Change, Service Request), each its own entity with
  its own fields and its own human-facing number series (`INC-000001`, `PRB-`, `RFC-`, `REQ-`),
  sharing one concrete `Ticket` record for the fields they all have. See
  [ADR-0001](docs/adr/0001-ticket-subtypes-composed-not-inherited.md) for why this is composition
  rather than JPA inheritance. A read-only cross-subtype view is available at `GET /api/tickets`.
- **Comments** — per ticket, with an `internal` flag that keeps agent-only notes away from the
  requester.
- **Classification** — categories (a tree) and priorities.
- **Custom fields** — administrators declare attribute definitions per deployment; values live in a
  `jsonb` column on the ticket and are validated on write. This is the product's per-customer
  customization mechanism, which is what makes PostgreSQL non-negotiable
  ([ADR-0002](docs/adr/0002-postgresql-only-product-owns-its-database.md)).
- **SLA** — response/resolution targets per priority, deadlines stamped onto the ticket, a first
  response recorded from the first non-internal agent comment, `PENDING` pausing the clock, and a
  Quartz-driven scanner that flags breaches exactly once.
- **Directory** — people (agents and customers in one entity, separated by role) and teams.
- **Security** — HTTP Basic today, with role-based access control and row-level ownership: a
  customer only ever sees tickets they requested, and a foreign ticket answers 404 rather than 403
  so ids can't be probed. An OAuth2/OIDC path is staged but inactive.

## Quick start

### Docker Compose — the whole stack, no local JDK

```bash
docker compose up --build
```

Brings up PostgreSQL and the application on `http://localhost:8080`. Create the first agent account
(this endpoint is open until exactly one person exists, then it refuses):

```bash
curl -X POST http://localhost:8080/api/setup \
  -H "Content-Type: application/json" \
  -d '{"name":"Admin","email":"admin@example.com","username":"admin","password":"admin123"}'
```

Every other endpoint needs credentials:

```bash
curl -u admin:admin123 http://localhost:8080/api/tickets
```

### `./mvnw spring-boot:test-run` — throwaway database, nothing to configure

```bash
./mvnw spring-boot:test-run
```

Boots the app through `TestServdeskApplication` against a Testcontainers-managed PostgreSQL that
disappears when you stop it — the fastest way to poke at a running instance, and runnable straight
from the IDE too. Needs Docker.

### `./mvnw spring-boot:run` — bring your own database

`application.properties` deliberately carries no connection settings, so supply them yourself:

```bash
docker run -d --name servdesk-db -p 5432:5432 \
  -e POSTGRES_DB=servdesk -e POSTGRES_USER=servdesk -e POSTGRES_PASSWORD=servdesk postgres:latest

./mvnw spring-boot:run \
  -Dspring-boot.run.arguments="--spring.datasource.url=jdbc:postgresql://localhost:5432/servdesk \
    --spring.datasource.username=servdesk --spring.datasource.password=servdesk"
```

Flyway creates the schema on first start; Hibernate is set to `validate` and will fail fast if the
mapping and the migration ever disagree.

## The API

The OpenAPI 3 contract at `src/main/resources/static/openapi/servdesk-api.yaml` is **hand-written
and authoritative** — controllers are written to match it, never the other way round. With the app
running, browse it at `http://localhost:8080/docs/index.html`.

- Successful responses are `application/hal+json` with hypermedia links (the one exception is the
  setup status, which has no links).
- Errors are RFC 7807 `ProblemDetail` documents — including the 401/403 that Spring Security
  produces before a request ever reaches a controller.
- Versioning is by header: `X-API-Version: 1`, defaulted, so existing client URLs stay stable.
- List endpoints take `?page=`, `?size=` and `?sort=`.

## Development

### Prerequisites

**Docker** is the only hard requirement: integration tests run against a real PostgreSQL via
Testcontainers, and Docker Compose is the simplest way to run the app. The Maven wrapper (`./mvnw`)
bootstraps Maven itself; a local **JDK 25** is only needed for IDE work.

### Tests

```bash
./mvnw test                            # everything
./mvnw test -Dtest=ClassName           # one class
./mvnw test -Dtest=ClassName#method    # one method
```

Two deliberately separate layers:

- **Unit tests** (`*CommandServiceTest`, `*QueryServiceTest`) mock every collaborator — no Spring
  context, no database, no Docker. They cover the decisions HTTP tests can't easily observe.
- **Integration tests** (`*ControllerTest` and friends) drive the real HTTP, security and
  persistence stack against a Testcontainers PostgreSQL. Nothing is mocked, because the point is
  proving that access control, soft deletes and error bodies actually behave as documented.

If Docker isn't available, the integration tests error out on container startup. That's the
environment talking, not the code.

### Build and quality gates

```bash
./mvnw verify                                   # the gate: tests, formatting, coverage report
./mvnw spotless:apply                           # fix formatting
./mvnw spotbugs:check                           # static analysis (not bound to verify)
./mvnw org.owasp:dependency-check-maven:check   # CVE scan (needs network access to the NVD feeds)
./mvnw package                                  # executable jar
docker build -t servdesk .                      # multi-stage, layered image
```

### CI

[`.github/workflows/ci.yml`](.github/workflows/ci.yml) runs three jobs on every push to `master` and
every pull request:

| Job | What it proves |
| --- | --- |
| `build-and-test` | `./mvnw verify` — compilation, unit and integration tests, ArchUnit layering rules, formatting, coverage. SpotBugs runs here too, non-blocking for now |
| `docker-build` | The `Dockerfile` still builds |
| `contract-tests` | The running app hasn't drifted from the OpenAPI contract — Redocly lints and bundles the spec, then Schemathesis exercises the live instance against it |

`contract-tests` is a blocking gate and has repeatedly caught things no hand-written test did, since
the test suite asserts mostly on status codes rather than response body shape.

## Architecture at a glance

A modular monolith under `dev.alkolhar.servdesk`, one package per feature:

| Package | Contents |
| --- | --- |
| `ticket` | The ticketing core: the shared `Ticket`, the four subtypes in their own subpackages, comments, and the read-only cross-subtype `overview` |
| `directory` | People and teams |
| `classification` | Categories and priorities |
| `customfield` | Admin-defined attribute definitions and write-time validation |
| `sla` | Policies, deadline derivation, and the Quartz breach scanner |
| `setup` | The one-shot first-agent bootstrap endpoint |
| `config` | Security, API versioning, JPA auditing |
| `common` | `BaseEntity` (auditing, optimistic locking, soft delete), domain events, HTTP-agnostic exceptions, RFC 7807 handling |

Two rules hold everywhere:

- **JPA entities never cross the HTTP boundary.** Each aggregate has three type families: the entity
  (persistence only), request records (validation, related entities as plain ids), and a response
  model carrying hypermedia links. A controller returns a model, never an entity. This one is a
  convention rather than a test — the obvious ArchUnit rule was tried and dropped, because
  bytecode-level dependency analysis can't tell the recommended
  `assembler.toModel(queryService.findById(id))` chain apart from the leak it's meant to forbid.
- **Feature packages stay free of cycles**, controllers never reach a repository directly, and
  services stay free of web-layer types. These three *are* enforced, by
  `architecture.ArchitectureTest`. Where a dependency would close a loop — `sla` already depends on
  `ticket`, so `ticket` must not depend back on `sla` — the interface is declared in the *calling*
  package and implemented by the one it would otherwise have to import. `ticket.SlaHooks`,
  implemented by `sla.TicketSlaService`, is the worked example.

Authorization is split by nature rather than scattered: static URL and role rules live in
`SecurityConfig`, while anything that depends on the data itself (row-level ownership, the internal
comment rule) lives in the service layer.

## Documentation map

| Document | Purpose |
| --- | --- |
| [`docs/`](docs/README.md) | Index of everything below, with an ADR listing |
| [`CLAUDE.md`](CLAUDE.md) | The deep reference: every design decision with its reasoning, and the traps behind them. Written for coding agents, useful to humans |
| [`CONTEXT.md`](CONTEXT.md) | Domain glossary — the project's ubiquitous language, and the synonyms to avoid |
| [`docs/adr/`](docs/adr/) | Architecture decision records for choices that would be expensive to reverse |
| [`docs/agents/`](docs/agents/) | Working agreements: issue tracking, triage labels, PR workflow |

This README is the front door; `CLAUDE.md` is the reference behind it.

## Contributing

Work lands through a feature branch and a pull request — never a direct commit to `master`. See
[`docs/agents/pr-workflow.md`](docs/agents/pr-workflow.md).
