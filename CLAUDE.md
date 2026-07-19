# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project status

servdesk is an ITSM (IT service management) ticketing application. Domain model, REST layer, security,
and CI are in place (see below); newer surface area (e.g. Category/Priority CRUD, OpenAPI contract
tests) is tracked in GitHub Issues, not this file.

## Commands

Build/run via the Maven wrapper (no system Maven required):

```
./mvnw compile                 # compile
./mvnw test                    # run all tests
./mvnw test -Dtest=ClassName   # run a single test class
./mvnw test -Dtest=ClassName#methodName   # run a single test method
./mvnw spring-boot:run         # run the app locally
./mvnw package                 # build the jar
```

Running the app or its integration tests requires Docker (Testcontainers — see Testing below).

## Architecture

- **Java 25**, **Spring Boot 4.1.0**, Maven build. Base package: `dev.alkolhar.servdesk`.
- **Database**: MariaDB (`mariadb-java-client`) + **Flyway** (`flyway-mysql`); migrations under
  `src/main/resources/db/migration`. Data access via **Spring Data JPA**.
- **Web**: Spring MVC + **Spring HATEOAS** for hypermedia responses.
- **Security**: `spring-boot-starter-security` + `spring-security-messaging`.
- **Spring Integration** (`http`/`jpa`) and **Quartz** are on the classpath for future integration
  flows and scheduled jobs, not yet used.
- **Validation**: Jakarta Bean Validation. **Actuator**: health/metrics. **DevTools**: local hot reload.
- **Code quality tooling** (Maven plugins, no new runtime deps except `jspecify`):
  - **Spotless** (Eclipse JDT formatter, tab-indented) enforces formatting on `./mvnw verify`;
    `./mvnw spotless:apply` to auto-fix.
  - **SpotBugs** is configured but not bound to `verify` — run manually via `./mvnw spotbugs:check`.
    26 findings (mostly `EI_EXPOSE_REP`/`EI_EXPOSE_REP2` on entity getters/setters) are untriaged.
  - **JaCoCo** reports coverage on every `test` run (`target/site/jacoco/index.html`); no threshold enforced.
  - **OWASP dependency-check-maven** is declared but not bound to a phase (needs network access) —
    run via `./mvnw org.owasp:dependency-check-maven:check`.
  - **ArchUnit** (`archunit-junit5`) backs `architecture.ArchitectureTest` — see Testing below.

### Layering: entities never cross the HTTP boundary

JPA entities (`Person`, `Ticket`, ticket subtypes) are persistence-only — never bound to a
`@RequestBody`, never returned from a controller. (Binding a request straight to an entity lets a
client-supplied `{"id": 2}` reach Hibernate as a detached instance Hibernate can't reconcile;
serializing an entity leaks Hibernate proxy internals into JSON.) Each aggregate has three type families:

- **Entity** — plain JPA, DB constraints only, no validation/Jackson annotations.
- **Request DTOs** (`*CreateRequest`/`*UpdateRequest`) — records; related entities as plain `Long` ids
  (`teamId`, `requesterId`, ...), never nested objects. Validation (`@NotBlank`/`@NotNull`/`@Email`)
  lives here. The service layer resolves ids via `EntityManager.getReference(Class, id)`.
- **Response model** (`*Model`) — extends HATEOAS `RepresentationModel`; assembled by a
  `RepresentationModelAssembler` (`*ModelAssembler`) that attaches hypermedia links. `PersonModel` has
  no `password` field — never put there, not filtered out.

Controllers return a `*Model` (or `CollectionModel<...>`/`PagedModel<...>`), never the entity.

### Domain model / packages

- `common` — cross-cutting building blocks:
  - `BaseEntity` (`@MappedSuperclass`): id, `createdAt`/`updatedAt` (Hibernate), `@Version` (optimistic
    locking), `createdBy`/`updatedBy` (Spring Data auditing against `SecurityContextHolder` —
    `createdBy` for the first agent is `"anonymousUser"`, not null, since `AnonymousAuthenticationFilter`
    is on by default), `deletedAt` (soft-delete marker). All fields `@Nullable` (genuinely null on a
    transient instance).
  - **Soft delete**: every concrete entity carries its own `@SQLDelete` + `@SQLRestriction("deleted_at
    IS NULL")` pair (can't live on `BaseEntity` — each needs its own table name in the SQL string).
    `@SQLRestriction` applies to every load including association fetches, so a soft-deleted agent can
    no longer authenticate. **Trade-off**: MariaDB has no partial/filtered unique index, so a
    soft-deleted row's unique columns (email/username/team name/priority name/ticket display numbers)
    still occupy the index — recreating with the same value throws `DataIntegrityViolationException`
    (mapped to 409 by `RestExceptionHandler`, not a domain exception, since the service layer never
    sees it coming).
  - `event.DomainEvent` — marker interface for `ApplicationEventPublisher` events (no broker).
    `event.DomainEventLogger` is a `@TransactionalEventListener` placeholder (logs at DEBUG) proving the
    wiring end-to-end — future SLA/notification listeners attach to the same base type.
    `@TransactionalEventListener`, not `@EventListener`: only fires after the publishing transaction commits.
  - `exception.NotFoundException`/`ConflictException`/`ForbiddenException` — unchecked, HTTP-agnostic;
    one exception per failure *kind*, not one subclass per aggregate. `ForbiddenException` is for
    data-dependent rejections a static URL+role rule can't express (e.g. a Customer marking their own
    comment internal) — distinct from `SecurityConfig`'s static rules. Keeping these free of HTTP types
    is what lets `ArchitectureTest.command_and_query_services_stay_free_of_web_layer_types` hold.
  - `web.RestExceptionHandler` — the only place that translates those exceptions to RFC 7807
    `ProblemDetail` HTTP responses (not `sendError`, so these never trigger Tomcat's `/error` forward —
    see `SecurityConfig` below). Also maps `DataIntegrityViolationException` → 409.
- `directory` — `Person` (single entity for agents and customers, distinguished by `role`;
  `username`/`password`/`enabled` only populated for login-capable people), `Team`.
  `PersonCommandService`/`PersonQueryService` — CQRS-light split (command depends on query, not vice
  versa); command handles create/update/delete/`createInitialAgent` (BCrypt-hashes password, publishes
  `PersonCreatedEvent`). `PersonUserDetails(Service)` adapts `Person` to Spring Security, mapping
  `role` → `ROLE_*`; defining this bean is what makes Boot back off its default generated-password setup.
  `PersonRepository.findByUsername` returns empty for customers (who typically have none).
- `classification` — ticket lookup/reference data: `Category` (self-referencing tree), `Priority`
  (name + `sortOrder`). No REST endpoints yet (tracked in an issue).
- `ticket` — the ticketing core, split per [ADR-0001](docs/adr/0001-ticket-subtypes-composed-not-inherited.md):
  - `Ticket` — concrete entity holding fields common to every subtype: `status`, `subject`,
    `description`, `category`/`priority` (nullable), `requester` (required), `assignee`/`team`
    (nullable), `resolvedAt`/`closedAt`. No `type` field — a subtype's existence says what kind it is.
  - `Incident`/`Problem`/`Change`/`ServiceRequest` (`ticket.incident`/`.problem`/`.change`/
    `.servicerequest`) — each an independent `@Entity` sharing `Ticket`'s PK via `@OneToOne @MapsId`
    (using `MapsIdBaseEntity`, not `BaseEntity`, since `@GeneratedValue(IDENTITY)` conflicts with
    `@MapsId`). Each has its own `displayNumber` + DB sequence: `INC-`/`PRB-`/`RFC-`/`REQ-` (Change's
    table is `change_request` — `change` is a MariaDB reserved word), zero-padded by hand rather than
    `String.format` (default-locale `DecimalFormatSymbols` can substitute non-ASCII digits).
    `Incident.relatedProblem` is an optional many-to-one to `Problem` (no reverse query). Creating any
    subtype is Agent-only.
  - `AbstractTicketSubtypeCommandService`/`QueryService` — shared base: resolves
    requester/assignee/team/category/priority ids, derives `resolvedAt`/`closedAt` purely server-side
    from a status transition (set entering RESOLVED/CLOSED, cleared on reopen to an earlier stage —
    relies on `TicketStatus`'s enum order matching lifecycle order), publishes
    `TicketStatusChangedEvent` only when status actually changed. Delete soft-deletes both the subtype
    row and the shared `Ticket` row.
  - `TicketComment` — references the shared `Ticket` directly (not per-subtype); `internal` flag
    distinguishes agent-only notes from requester-visible replies. `CommentController`
    (`/api/tickets/{ticketId}/comments`, GET+POST only) is nested under the shared id for this reason.
    `CommentCommandService` rejects (`ForbiddenException`, 403) `internal=true` from a Customer;
    `CommentQueryService` filters internal comments out of a Customer's read.
- `setup` — `SetupController` (`GET`/`POST /api/setup`, `permitAll`) bootstraps the first agent;
  `createInitialAgent`/`isSetupRequired` refuse to run once any `Person` exists (409) — no seeded
  credentials ship in a migration.
- `config.SecurityConfig` — auth and authz are separate concerns (swapping auth mechanisms shouldn't
  touch RBAC rules):
  - CSRF disabled — HTTP Basic on every request, no cookies/sessions; CSRF's filter runs before Basic
    Auth, so left enabled it rejects valid writes before credentials are checked.
  - **RBAC**: person directory (`/api/persons/**`) is Agent-only, all methods. Every ticket subtype:
    `GET` open to both roles, `POST`/`PUT`/`DELETE` Agent-only. Comments: `GET`/`POST` open to both
    (the `internal`-is-Agent-only rule is enforced in the service layer, not here, since it's
    data-dependent). Verified against the real filter chain in `PersonControllerTest` and
    `AbstractTicketSubtypeControllerTest.customersCanReadButNotCreateUpdateOrDelete`. Out of scope:
    row-level ownership (a customer seeing only their own tickets) — needs a data-access decision the
    service layer would make, not a URL+role rule.
  - **OAuth2/OIDC migration path**: `spring-boot-starter-oauth2-resource-server` is on the classpath but
    inert until `issuer-uri` is set. Migrating swaps `.httpBasic(...)` for
    `.oauth2ResourceServer(oauth2 -> oauth2.jwt(...))` and replaces `PersonUserDetailsService` with a
    `JwtAuthenticationConverter`; `authorizeHttpRequests` wouldn't change. Not wired up — no IdP in this
    environment to verify against yet.
  - `/api/setup/**`, `/openapi/**`+`/docs/**`+`/webjars/**`, and `/error` are `permitAll`. `/error`
    matters because any `sendError(...)`-based response (raw `ResponseStatusException`, an unmapped
    404) triggers Tomcat's internal forward to `/error`, re-entering the whole filter chain with no
    credentials on an unauthenticated request — without this entry that forward gets rejected and
    silently overwrites the real status with 401. `NotFoundException`/`ConflictException`/
    `ForbiddenException`/`DataIntegrityViolationException` don't hit this path — `RestExceptionHandler`
    answers them directly as `ProblemDetail` without calling `sendError`.
- Migrations: a single `V1__init_schema.sql` under `src/main/resources/db/migration` creates the full
  schema (audit/soft-delete columns, each subtype's `*_number_seq`). Once anything is deployed, switch
  back to versioned migrations rather than editing this one.
- `application.properties`: `ddl-auto=validate` (Flyway is the only schema source of truth; this just
  fails fast on drift — e.g. a `@Lob String` needs an explicit `length` or Hibernate caps it at 255
  chars, see `TicketComment.body`'s `@Column`) and `open-in-view=false` (assemblers only call `.getId()`
  on lazy associations, which a proxy answers without an open session).
- **Null-safety**: every package has `package-info.java` with `@NullMarked` (JSpecify) — annotations
  don't apply to subpackages, so each `ticket.*` subpackage carries its own. No enforcing tool
  (NullAway/Error Prone) wired in yet — documentation/IDE-hint level only.

### API architecture

- **OpenAPI contract-first, no codegen**: `src/main/resources/static/openapi/servdesk-api.yaml` is
  hand-authored and is the source of truth; controllers are written to match it. Served as a static
  resource, viewable at `/docs/index.html` via a Swagger UI webjar (deliberately not `springdoc-openapi`,
  which generates the spec from annotations — the opposite direction). Webjar version is hardcoded in
  both `pom.xml` and `docs/index.html` — keep in sync if bumped. (No automated check that the contract
  and implementation agree yet — tracked in an issue.)
- **API versioning**: header-based (`X-API-Version`) via Spring Framework 7's native
  `@RequestMapping(version = "...")`, declared once at the class level (`config.ApiVersioningConfig`,
  `setDefaultVersion("1")`). `SetupController` is deliberately unversioned (pre-auth bootstrap). An
  unsupported version is rejected with 400.
- **Pagination + sorting**: list endpoints take a `Pageable` (bound from `?page=&size=&sort=`) and a
  `org.springframework.data.web.PagedResourcesAssembler<T>` (from `spring-data-commons`, not
  `org.springframework.hateoas.*`), which builds `first`/`self`/`next`/`last` links and page metadata
  from the request URI. `CommentController.findAll` is deliberately *not* paginated
  (`CollectionModel`, not `PagedModel`) — it reads as one combined chronological stream.
- **Filtering**: `PersonQueryService.findAll` takes an optional `role`; each ticket subtype's `findAll`
  takes an optional `status` (no `type` filter — each subtype's endpoint is already scoped to one type).

### Deployment

- `Dockerfile` — multi-stage: build with `eclipse-temurin:25-jdk`, extract the Spring Boot layered jar
  (`java -Djarmode=tools -jar app.jar extract --layers --launcher`) into
  `dependencies`/`spring-boot-loader`/`snapshot-dependencies`/`application` layers, copy into an
  `eclipse-temurin:25-jre` runtime image most-to-least-stable, run as non-root `servdesk`.
- `docker-compose.yml` — `app` + `db` (MariaDB) for local dev; `docker compose up --build` needs no
  local JDK at all.
- `.github/workflows/ci.yml` — `./mvnw verify` (compile, unit + Testcontainers integration tests,
  ArchUnit, Spotless, JaCoCo) plus a `docker build` validation job. SpotBugs runs with
  `continue-on-error: true` (26 untriaged findings) — remove the flag once those are resolved.

### Testing

Two deliberately separate layers:

- **Unit tests** (`*CommandServiceTest`/`*QueryServiceTest`) — `@ExtendWith(MockitoExtension.class)`,
  every collaborator mocked, no Spring context/database. Cover internal decisions HTTP tests can't
  easily observe: does `update()` leave an unset password alone, does status-transition derivation of
  `resolvedAt`/`closedAt` only fire on an actual transition (exercised via `ProblemCommandService`,
  since Problem carries no field of its own — the simplest concrete subtype), is a rejection thrown
  *before* any repository call. A transient entity has no public id setter, so tests needing a "saved"
  fixture use `ReflectionTestUtils.setField(entity, "id", ...)` inside the mocked repository's `save`
  stub.
- **Integration tests** (`*ControllerTest`, `SetupControllerTest`) — **Testcontainers**
  (`TestcontainersConfiguration`, `MariaDBContainer`, `@ServiceConnection`; `public` so test classes
  outside the root package can `@Import` it). Full `@SpringBootTest(webEnvironment = RANDOM_PORT)` +
  `TestRestTemplate` (lives in `org.springframework.boot.resttestclient`; needs
  `spring-boot-starter-restclient` + `@AutoConfigureTestRestTemplate` explicitly — Boot no longer
  auto-registers it) driving the real HTTP + security + persistence stack — nothing mocked, since the
  point is proving RBAC, soft-delete `@SQLRestriction`, and RFC 7807 bodies actually behave as
  documented. Docker must be running.
  - `AbstractTicketSubtypeControllerTest` (package `ticket`) is the shared base ADR-0001 calls for:
    CRUD round-trip, per-role RBAC, display-number format, `resolvedAt`/`closedAt` derivation,
    soft-delete, and comment internal-flag enforcement — exercised once per subtype.
    `IncidentControllerTest`/`ProblemControllerTest`/etc. extend it, overriding only `basePath()`/
    `expectedDisplayNumberPrefix()`. `IncidentRelatedProblemTest` covers Incident-only behavior separately.
  - `SetupControllerTest` needs an empty database, so it's `@DirtiesContext(classMode = AFTER_CLASS)`
    to force its own container. `PersonControllerTest`/`AbstractTicketSubtypeControllerTest` carry the
    same annotation, and use `@TestInstance(PER_CLASS)` + `@BeforeAll` to bootstrap an agent through
    the real `/api/setup` endpoint once per class.
- `TestServdeskApplication` — dev-time entry point booting the app with Testcontainers applied, for
  running locally against a disposable MariaDB without Docker Compose.
- `architecture.ArchitectureTest` (ArchUnit, `@AnalyzeClasses` over the whole base package) freezes the
  layering rules above as executable checks: feature packages stay cycle-free, controllers never
  depend on a `*Repository`, `*CommandService`/`*QueryService` never depend on `org.springframework.web..`.
  ("Controllers must never reference an `@Entity`" was considered and dropped — ArchUnit's bytecode
  resolution flags the transient `assembler.toModel(queryService.findById(id))` chain as a dependency
  on the entity, failing the exact pattern this codebase recommends.)
- **Not yet built**: OpenAPI contract-driven API tests and CI wiring for them (tracked in issues).

## Agent skills

### Issue tracker

Issues live in GitHub Issues (github.com/alkolhar/servdesk), using the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

Default five-role vocabulary (`needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`). See `docs/agents/triage-labels.md`.

### Domain docs

Single-context — one `CONTEXT.md` + `docs/adr/` at the repo root. See `docs/agents/domain.md`.

### PR workflow

Issue-driven work lands via a feature branch + pull request, never a direct commit to `master`.
See `docs/agents/pr-workflow.md`.
