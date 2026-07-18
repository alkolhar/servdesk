# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project status

servdesk is an ITSM (IT service management) ticketing application. The domain model is still small (see below); most of the surrounding
app (repositories, REST endpoints, security config) has not been built yet.

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

Running the app or its integration tests requires Docker to be available — see Testcontainers note below.

## Architecture

- **Java 25**, **Spring Boot 4.1.0**, Maven build (`spring-boot-starter-parent`).
- Base package: `dev.alkolhar.servdesk`.
- **Database**: MariaDB in production (via `mariadb-java-client`), with **Flyway** (`flyway-mysql`)
  for schema migrations — migrations belong under `src/main/resources/db/migration`. Data access is
  via **Spring Data JPA**.
- **Web layer**: Spring MVC (`spring-boot-starter-webmvc`) with **Spring HATEOAS** for hypermedia-driven
  REST responses.
- **Security**: `spring-boot-starter-security` plus `spring-security-messaging` (message-based security,
  relevant if Spring Integration endpoints need securing).
- **Spring Integration**: `spring-integration-http` and `spring-integration-jpa` are included, suggesting
  integration flows (e.g. polling JPA-backed channels, HTTP inbound/outbound gateways) are part of the
  intended design.
- **Quartz** (`spring-boot-starter-quartz`) is included for scheduled/cron jobs, backed by the same
  datasource.
- **Validation**: `spring-boot-starter-validation` (Jakarta Bean Validation) for request/entity validation.
- **Actuator**: `spring-boot-starter-actuator` for health/metrics endpoints.
- **DevTools** is on the runtime classpath for local hot reload.
- **Code quality tooling** (Maven plugins, no new runtime dependencies except `jspecify`):
  - **Spotless** with the Eclipse JDT formatter enforces formatting on `./mvnw verify`; run
    `./mvnw spotless:apply` to auto-fix. Palantir Java Format was tried first but its javac-tree
    bridge breaks on very new JDKs (`NoSuchMethodError` against `javac` internals) — Eclipse's
    formatter doesn't share that coupling. The existing code is tab-indented (Eclipse default), not
    space-indented — `.editorconfig` matches this.
  - **SpotBugs** is configured but *not* bound to any lifecycle phase: its bundled ASM can't parse
    class files newer than what it was built against, which breaks on a from-source JDK ahead of
    what SpotBugs has caught up to (fails analyzing the JDK's own platform classes, independent of
    this project's own `--release` target). Run manually (`./mvnw spotbugs:check`) once the
    environment has a compatible JDK/SpotBugs pairing.
  - **JaCoCo** reports coverage on every `test` run (`target/site/jacoco/index.html`); no minimum
    threshold is enforced yet.
  - **OWASP dependency-check-maven** is declared but not bound to a phase (needs network access to
    the NVD feed and is too slow for every local build) — run explicitly via
    `./mvnw org.owasp:dependency-check-maven:check`.
  - **ArchUnit** (`archunit-junit5`, test scope) backs `dev.alkolhar.servdesk.architecture.ArchitectureTest` — see Testing below.

### Layering: entities never cross the HTTP boundary

JPA entities (`Person`, `Ticket`, etc.) are persistence-only — they're never bound to a `@RequestBody`
and never returned directly from a controller. Two problems that caused in an earlier iteration and
why the split exists now:

- Binding a `@RequestBody` straight to an entity let a client-supplied `{"id": 2}` reach Hibernate as
  a manually-constructed "detached" instance with a null `@Version`, which Hibernate can't safely
  reconcile against the real row (`PropertyValueException`/`DataIntegrityViolationException`).
- Serializing an entity with lazy associations leaks Hibernate's proxy internals
  (`hibernateLazyInitializer`) into the JSON response.

So each aggregate has three families of type instead of one:

- **Entity** (`Person`, `Ticket`) — plain JPA, DB constraints only, no Jakarta Validation, no Jackson
  annotations.
- **Request DTOs** (`PersonCreateRequest`/`PersonUpdateRequest`, `TicketCreateRequest`/
  `TicketUpdateRequest`) — Java records; carry related entities as plain `Long` ids (`teamId`,
  `requesterId`, ...), never nested objects. Validation (`@NotBlank`/`@NotNull`/`@Email`) lives here.
  The service layer resolves those ids to managed references via `EntityManager.getReference(Class,
  id)` — a proxy that carries only the id, safe to attach to a new/updated owning entity without a
  round trip, and without the detached-entity trap above.
- **Response model** (`PersonModel`, `TicketModel`) — extends Spring HATEOAS's `RepresentationModel`;
  assembled from an entity by a `RepresentationModelAssembler` (`PersonModelAssembler`,
  `TicketModelAssembler`) that also attaches hypermedia links (`self`, and on `TicketModel`,
  `requester`/`assignee` links to the corresponding `PersonController` resource). `PersonModel` has no
  `password` field at all — it's not filtered out, it was never put there.

Controllers return `PersonModel`/`TicketModel` (or `CollectionModel<...>` for list endpoints), never
the entity.

### Domain model / packages

- `common` — cross-cutting building blocks shared by every feature package:
  - `BaseEntity` — shared `@MappedSuperclass` (id, `createdAt`/`updatedAt` via Hibernate
    `@CreationTimestamp`/`@UpdateTimestamp`, `@Version` for optimistic locking, `createdBy`/
    `updatedBy` via Spring Data's `@CreatedBy`/`@LastModifiedBy` + `@EntityListeners
    (AuditingEntityListener.class)`, and a `deletedAt` soft-delete marker). All entities extend
    this. `id`/`createdAt`/`updatedAt`/`createdBy`/`updatedBy`/`version` are all `@Nullable`
    (JSpecify) — genuinely so, since they're null on a transient instance before Hibernate/the
    auditing listener populate them on insert.
  - `config.JpaAuditingConfig` wires `@EnableJpaAuditing` to an `AuditorAware<String>` reading
    `SecurityContextHolder`. Spring Security's `AnonymousAuthenticationFilter` is on by default (see
    `SecurityConfig`), so even the permitAll `/api/setup` flow has a principal — `createdBy` for the
    very first agent is literally `"anonymousUser"`, not null. The `Optional.empty()` branch only
    matters for a write with no `SecurityContext` at all (a future non-HTTP path, e.g. a Quartz job).
  - **Soft delete**: every concrete entity (`Person`, `Team`, `Category`, `Priority`, `Ticket`,
    `TicketComment`) carries its own `@SQLDelete(sql = "UPDATE <table> SET deleted_at = ... WHERE id
    = ? AND version = ?")` + `@SQLRestriction("deleted_at IS NULL")` pair — these can't live on
    `BaseEntity` itself since each needs its own table name baked into the SQL string. `@SQLRestriction`
    applies to every load of that entity type, including via association fetches and
    `PersonRepository.findByUsername`, so a soft-deleted agent can no longer authenticate.
    **Known trade-off**: MariaDB has no partial/filtered unique index, so a soft-deleted row's unique
    columns (`person.email`/`username`, `team.name`, `priority.name`, `ticket.ticket_number`) still
    occupy the index — recreating a new row with the same value throws `DataIntegrityViolationException`,
    mapped by `RestExceptionHandler` to a 409 `ProblemDetail` (not a domain-level `ConflictException`,
    since the service layer never sees this coming — it surfaces as a DB constraint violation).
  - `event.DomainEvent` — marker interface for events published via Spring's own
    `ApplicationEventPublisher` (no separate event bus/broker). Deliberately not sealed: feature
    packages define their own concrete event types (`directory.event`, `ticket.event`) without
    `common` needing to know about them. `event.DomainEventLogger` is a `@TransactionalEventListener`
    placeholder that proves the wiring end-to-end (logs at DEBUG) — future SLA/Notifications modules
    add their own listeners on the same base type rather than replacing this one. Using
    `@TransactionalEventListener` (not plain `@EventListener`) matters: it only fires after the
    publishing transaction commits, so a listener never reacts to a change that was rolled back.
  - `exception.NotFoundException`/`exception.ConflictException` — unchecked, HTTP-agnostic exceptions
    thrown by command/query services (e.g. `PersonNotFoundException`-shaped situations use
    `NotFoundException` directly rather than one subclass per aggregate, since the two feature
    modules don't yet have enough distinct failure modes to justify a deeper hierarchy). Keeping them
    free of any HTTP concept is what lets `ArchitectureTest.command_and_query_services_stay_free_of_web_layer_types`
    hold.
  - `web.RestExceptionHandler` — the *only* place that translates those exceptions to HTTP statuses.
    Returns RFC 7807 `ProblemDetail` bodies directly from each `@ExceptionHandler` method (Spring's
    built-in support, not a hand-rolled shape) rather than calling `HttpServletResponse.sendError(...)`
    as an earlier version did — this means `NotFoundException`/`ConflictException` no longer trigger
    Tomcat's internal `/error` forward at all; the response is written within the original,
    already-authorized request. `/error` stays permitAll in `SecurityConfig` regardless, since it's
    still reached by errors this class doesn't handle (an unmapped URL's 404, or any other uncaught
    exception) and by Spring Boot's own default `MethodArgumentNotValidException` handling (already
    `ProblemDetail`-shaped without any code here, since that exception implements `ErrorResponse`).
    Also handles `DataIntegrityViolationException` → 409, which is what a soft-deleted row's still-occupied
    unique column (see below) actually surfaces as.
- `directory` — who's involved:
  - `Person` — single entity for both agents and customers, distinguished by `role`
    (`PersonRole`: `AGENT`/`CUSTOMER`). `username`/`password`/`enabled` are only populated for people
    who can log in (agents today, potentially customers later via a self-service portal); finer-grained
    permissions (e.g. admin vs regular agent) are meant to be handled by Spring Security authorities,
    not by `PersonRole`. `team` is only meaningful for agents.
  - `PersonCommandService`/`PersonQueryService` — CQRS-light split of what used to be a single
    `PersonService`: `PersonCommandService` handles `create`/`update`/`delete`/`createInitialAgent`
    (BCrypt-hashes `password` before storage on both create and update, publishes
    `event.PersonCreatedEvent` after a successful create), `PersonQueryService` handles
    `findAll`/`findById`/`isSetupRequired`. The command service depends on the query service (e.g. to
    load-then-mutate on `update`, or to check `isSetupRequired` before `createInitialAgent`) rather
    than the other way around. This is *preparation* for CQRS, not a full read/write model split —
    there's one schema and one repository per aggregate; the split exists so a later, real read-model
    optimization (projections, caching, a separate query path) doesn't require touching the command
    side.
  - `PersonUserDetails`/`PersonUserDetailsService` — adapt a login-capable `Person` to Spring
    Security's `UserDetails`/`UserDetailsService`, mapping `role` to a `ROLE_*` `GrantedAuthority`.
    Defining this `UserDetailsService` bean makes Spring Boot back off its default
    single-user/generated-password setup, so authentication is against `person.username`/`password`
    rows from the moment this bean exists — see the `setup` package below for how the first one gets
    created.
  - `PersonRepository.findByUsername` — used by `PersonUserDetailsService`; returns empty (not just a
    null-password match) for customers, who typically have no `username`.
  - `Team` — group agents can be assigned to.
- `classification` — ticket lookup/reference data: `Category` (self-referencing parent/child tree),
  `Priority` (name + `sortOrder`).
- `ticket` — the ticketing core:
  - `Ticket` — one entity for all ticket kinds, discriminated by `type` (`TicketType`: `INCIDENT`,
    `SERVICE_REQUEST`, `PROBLEM`, `CHANGE`) rather than separate entities per type. Has `status`
    (`TicketStatus`: `OPEN`, `IN_PROGRESS`, `PENDING`, `RESOLVED`, `CLOSED`), `requester` (`Person`,
    required), `assignee` (`Person`, nullable), `team` (nullable), `category`/`priority` (nullable).
    `ticketNumber` (`TCK-000123`) is drawn from `ticket_number_seq` in `TicketCommandService.create` —
    it has to be assigned before the row is inserted (the column is `NOT NULL`), and the id isn't
    known until after insert, so it can't be derived from the id.
  - `TicketComment` — activity/conversation entries on a ticket; `internal` flag distinguishes
    agent-only notes from replies visible to the requester.
  - `TicketCommandService`/`TicketQueryService` — same CQRS-light split as `directory`.
    `TicketCommandService.create` publishes `event.TicketCreatedEvent`; `update` publishes
    `event.TicketStatusChangedEvent` *only* when the status actually changed (compares the status
    before and after mutating), since that transition — not every field edit — is the intended hook
    point for future SLA-timer and notification modules.
- `setup` — first-run bootstrap: `SetupController` (`GET /api/setup` for status, `POST /api/setup` to
  create the first agent) is `permitAll` in `SecurityConfig`, since an empty database has no `Person`
  to authenticate as. `PersonService.createInitialAgent`/`isSetupRequired` refuse to run a second time
  once any `Person` exists (409 Conflict), so the endpoint can't be used to mint extra accounts later —
  no seeded/default credentials ship in a migration.
- `config.SecurityConfig` — authentication and authorization are deliberately two separate concerns,
  so swapping the authentication mechanism (see the OAuth2/OIDC note below) never touches the RBAC
  rules, and so authorization stays centralized here instead of scattered across `@PreAuthorize` in
  the service layer:
  - CSRF is disabled — this API authenticates via HTTP Basic on every request, not cookies/sessions,
    so CSRF protection has nothing to guard and — worse — its filter runs *before* Basic Auth, so left
    enabled it rejects valid POST/PUT/DELETE requests before credentials are even checked.
  - **RBAC**: `PersonUserDetailsService` maps `Person.role` to a `ROLE_AGENT`/`ROLE_CUSTOMER`
    `GrantedAuthority`; `authorizeHttpRequests` is the first thing that actually reads it. Policy: the
    person directory (`/api/persons/**`, all methods) is AGENT-only; tickets can be read (`GET`) and
    raised (`POST`) by either role, but only an AGENT can change status or delete (`PUT`/`DELETE`).
    Verified in `PersonControllerTest.customersCannotManageThePersonDirectory` and
    `TicketControllerTest.customersCanReadAndCreateButNotModifyOrDeleteTickets` — both create a real
    customer login and hit the real filter chain, not a mock. Deliberately out of scope: row-level
    ownership (a customer seeing only *their own* tickets/profile) — that needs the caller's identity
    compared against the loaded resource, a data-access decision the service layer would have to make,
    not a static URL+role rule.
  - **OAuth2/OIDC migration path**: `spring-boot-starter-oauth2-resource-server` is already on the
    classpath (inert — Boot's autoconfiguration only activates once
    `spring.security.oauth2.resourceserver.jwt.issuer-uri` is set). Migrating means swapping
    `.httpBasic(withDefaults())` for `.oauth2ResourceServer(oauth2 -> oauth2.jwt(withDefaults()))` and
    replacing `PersonUserDetailsService` with a `JwtAuthenticationConverter` mapping a claim to the
    same `ROLE_*` authorities — `authorizeHttpRequests` wouldn't change at all. Not wired up as an
    actual alternate filter chain yet: there's no IdP in this environment to verify it against, and an
    unverified security config is worse than a documented gap. Stand up an IdP (e.g. Keycloak via
    Docker Compose, see Deployment) before flipping this switch.
  - `/api/setup/**`, `/openapi/**` + `/docs/**` + `/webjars/**` (the hand-written OpenAPI contract and
    its Swagger UI viewer, see API Architecture below), and `/error` are `permitAll`. The `/error`
    entry is easy to skip and easy to get bitten by: any error response produced via
    `response.sendError(...)` — e.g. a raw `ResponseStatusException`, or Boot's default handling of a
    404 for a URL with no matching handler at all — triggers Tomcat's internal forward to `/error`,
    which re-enters the *entire* security filter chain. On an unauthenticated request that forward
    carries no credentials, so without this permitAll entry `AuthorizationFilter` rejects the forwarded
    `/error` request and silently overwrites the real status with 401. An authenticated request doesn't
    show this, because its Authorization header is preserved across the forward. Note this no longer
    covers `NotFoundException`/`ConflictException`/`DataIntegrityViolationException` — those are
    answered directly as a `ProblemDetail` body by `RestExceptionHandler` without ever calling
    `sendError`, so they never reach `/error` at all (verified against the exact unauthenticated
    409-on-setup scenario in `SetupControllerTest`, which still passes).
  - Also defines the `PasswordEncoder` (`BCryptPasswordEncoder`) bean `PersonService` and the
    auto-wired `DaoAuthenticationProvider` (built from the `PersonUserDetailsService` bean) both rely on.
- Migrations live in `src/main/resources/db/migration`. A single `V1__init_schema.sql` creates the
  full schema above (including the audit/soft-delete columns and the `ticket_number_seq` sequence) —
  this used to be spread across four incremental migrations (V1-V4) written as the schema evolved
  phase by phase, folded back into one now that nothing has been released yet and there's no applied
  history to preserve. Once anything is actually deployed, go back to adding new versioned migrations
  rather than editing this one.
- `application.properties` sets `spring.jpa.hibernate.ddl-auto=validate` (Hibernate never generates or
  alters schema — Flyway is the only source of truth, this just fails fast if a mapping and the actual
  schema disagree) and `spring.jpa.open-in-view=false` (assemblers only ever call `.getId()` on lazy
  associations, which a Hibernate proxy always answers from its stored identifier without needing an
  open session — nothing here actually relies on open-in-view). Turning on `ddl-auto=validate` is what
  originally caught a mapping/schema drift here: a `@Lob String` with no explicit `@Column` maps to
  `LONGTEXT` in Hibernate 7's MariaDB dialect, but `@Column(nullable = false)` alone (no `length`
  given) pulls in
  JPA's `length` default of 255, which Hibernate reads as "cap this LOB at 255 chars" (TINYTEXT) — see
  the comment on `TicketComment.body`'s `@Column(nullable = false, length = Integer.MAX_VALUE)`.
- **Null-safety**: every package has a `package-info.java` with `@NullMarked` (JSpecify) — package
  annotations don't apply to subpackages in Java, so `directory.event`/`ticket.event`/
  `common.exception`/`common.web` each carry their own rather than inheriting one from their parent.
  Nullable fields/parameters/returns are explicitly `@Nullable`; there's deliberately no enforcing
  tool (no NullAway/Error Prone) wired in yet, so this is documentation/IDE-hint level only — a
  future addition, not assumed to hold today.

### API architecture

- **OpenAPI contract-first, no code generation**: `src/main/resources/static/openapi/servdesk-api.yaml`
  is hand-authored and is the source of truth for the API surface; controllers are written to match
  it, nothing is generated in either direction. It's served as a plain static resource (Spring Boot's
  default static-resource handling), viewable at `/docs/index.html` via a Swagger UI page built from
  the `org.webjars:swagger-ui` webjar's static assets only — deliberately **not** `springdoc-openapi`,
  which auto-generates a spec from annotations (the opposite of contract-first) and, being new to
  Spring Boot 4.1/Framework 7 at the time this was written, was an unnecessary compatibility risk for
  something this project doesn't actually need. The webjar version is hardcoded in both `pom.xml` and
  `docs/index.html`'s asset paths — keep them in sync if it's ever bumped.
- **API versioning**: header-based (`X-API-Version`), using Spring Framework 7's native
  `@RequestMapping(version = "...")` support (`config.ApiVersioningConfig`), not a hand-rolled
  URI-prefix scheme. Declared once at the class level on `PersonController`/`TicketController`
  (`@RequestMapping(value = "...", version = "1")`) — confirmed by reading `VersionRequestCondition`'s
  source that a method-level mapping without its own `version` inherits the class-level one, so
  individual `@GetMapping`/`@PostMapping` methods don't repeat it. `setDefaultVersion("1")` means a
  request that omits the header still resolves to version 1, so existing/other clients don't break.
  `SetupController` is deliberately left unversioned (pre-auth bootstrap, version negotiation doesn't
  apply). An unsupported version (e.g. `X-API-Version: 2`, since only "1" exists) is rejected with 400.
- **Pagination + sorting**: `PersonController.findAll`/`TicketController.findAll` take a `Pageable`
  (bound from `?page=&size=&sort=` automatically via Spring Data Web support, autoconfigured once
  Spring Data repositories are on the classpath — no `@EnableSpringDataWebSupport` needed) and a
  `org.springframework.data.web.PagedResourcesAssembler<T>` (**not** `org.springframework.hateoas.*` —
  it lives in `spring-data-commons`, autoconfigured as a controller-method-argument-resolvable bean
  once both Spring Data and Spring HATEOAS are present). The assembler builds `first`/`self`/`next`/
  `last` links and `page` metadata (`size`/`totalElements`/`totalPages`/`number`) directly from the
  current request URI — no manual `linkTo(methodOn(...))` needed for the collection resource, unlike
  the single-item endpoints.
- **Filtering**: `PersonQueryService.findAll` takes an optional `role` (`PersonRepository.findByRole`);
  `TicketQueryService.findAll` takes optional `status`/`type` via a single `@Query` with
  `(:param is null or ...)` clauses (`TicketRepository.findByOptionalFilters`) rather than a
  `Specification`-based dynamic-query framework — two optional filter dimensions didn't justify that
  machinery.

### Deployment

- `Dockerfile` — multi-stage: build with `eclipse-temurin:25-jdk` (`./mvnw package -DskipTests`;
  verified that JaCoCo's `report` execution, bound to the `test` phase, degrades gracefully to
  "Skipping JaCoCo execution due to missing execution data file" rather than failing when tests are
  skipped), extract the Spring Boot layered jar (`java -Djarmode=tools -jar app.jar extract
  --layers --launcher`) into `dependencies`/`spring-boot-loader`/`snapshot-dependencies`/
  `application` layers, copy them into an `eclipse-temurin:25-jre` runtime image in that
  most-to-least-stable order, run as a non-root `servdesk` user. `-Djarmode=tools` (not the older
  `-Djarmode=layertools`) and the exact `extract` flags were confirmed by actually running them
  against this project's built jar, not assumed from memory — Boot's jarmode CLI has changed across
  versions.
- `docker-compose.yml` — `app` + `db` (MariaDB) for local development; `docker compose up --build`
  is the fastest path to a running instance without a local JDK at all.
- `.github/workflows/ci.yml` — `./mvnw verify` (compile, unit + Testcontainers/MariaDB integration
  tests, ArchUnit, Spotless, JaCoCo — GitHub-hosted `ubuntu-latest` runners have Docker preinstalled,
  which Testcontainers needs) plus a separate `docker build` validation job. SpotBugs runs too, but
  with `continue-on-error: true`: it's never been observed passing in CI (the local dev machine used
  during this project's early phases has a JDK too new for SpotBugs's bundled ASM to analyze at all,
  so `spotbugs:check` was never actually exercised end-to-end there) — remove that flag once a real
  CI run confirms it's green, rather than assuming it works because the JDK version is technically
  older here.

### Testing

**Teststrategie**: two deliberately different layers, not one blended into the other.

- **Unit tests** (`PersonCommandServiceTest`, `PersonQueryServiceTest`, `TicketCommandServiceTest`,
  `TicketQueryServiceTest`) — plain `@ExtendWith(MockitoExtension.class)`, every collaborator
  (`*Repository`, `EntityManager`, `PasswordEncoder`, `ApplicationEventPublisher`) mocked, no Spring
  context, no database, each running in well under a second. These exist for internal decisions the
  HTTP-level tests can't easily observe or would only cover by accident: does `update()` leave an
  unset password alone, does `TicketCommandService.update()` publish `TicketStatusChangedEvent` only
  when the status actually changed (not on every save), is `createInitialAgent` rejected *before*
  ever calling `personRepository.save` once setup is already done. A transient JPA entity has no
  public id setter (`BaseEntity.id` is Hibernate-generated), so tests that need a "saved" fixture with
  a known id use `org.springframework.test.util.ReflectionTestUtils.setField(entity, "id", ...)` inside
  the mocked repository's `save(...)` stub — simulating what Hibernate would actually do, rather than
  constructing a separately-faked entity.
- **Integration tests** (`PersonControllerTest`, `TicketControllerTest`, `SetupControllerTest`) use
  **Testcontainers** with a `MariaDBContainer` (`TestcontainersConfiguration`, image `mariadb:latest`)
  wired in via `@ServiceConnection`, so JPA/Flyway-backed tests run against a real MariaDB container
  rather than mocks or an in-memory DB. Docker must be running for these tests to pass.
  `TestcontainersConfiguration` is `public` (not the Initializr-default package-private) specifically so
  test classes outside the root `dev.alkolhar.servdesk` package can `@Import` it. These drive the real
  HTTP + security + persistence stack end to end (e.g. `TicketControllerTest.customersCanReadAndCreateButNotModifyOrDeleteTickets`
  authenticates as a real customer login and hits the real `SecurityFilterChain`) — nothing here is
  mocked, which is exactly the point: it's what actually proves the RBAC rules, the soft-delete
  `@SQLRestriction`, and the RFC 7807 error bodies behave as documented, not just that the code compiles
  against a mocked collaborator.
- **Not yet built** (deliberately deferred, per the original plan): API tests driven from the OpenAPI
  contract itself (`static/openapi/servdesk-api.yaml`) verifying the implementation never drifts from
  it, and CI/CD wiring — both belong with the Deployment phase's GitHub Actions work, where there's
  actually a pipeline for them to run in.
- `TestServdeskApplication` is a dev-time entry point that boots the app with the Testcontainers
  configuration applied (useful for running the app locally against a disposable MariaDB instance without
  a separate Docker Compose setup).
- Controller-level tests (`PersonControllerTest`, `TicketControllerTest`, `SetupControllerTest`) are
  full `@SpringBootTest(webEnvironment = RANDOM_PORT)` + `TestRestTemplate` integration tests that drive
  the real HTTP stack (security included) rather than calling services directly or mocking anything —
  prefer extending this pattern over manually curling a locally-run instance to check behavior.
  - `TestRestTemplate` lives in `org.springframework.boot.resttestclient` in this Spring Boot version
    (4.1 split it out of `spring-boot-test`), and needs both `spring-boot-starter-restclient` (for
    `RestTemplateBuilder`, which isn't pulled in transitively by any of this project's other `-test`
    starters) and an explicit `@AutoConfigureTestRestTemplate` on the test class — unlike earlier Boot
    versions, `@SpringBootTest(webEnvironment = RANDOM_PORT)` alone no longer auto-registers the bean.
  - `SetupControllerTest` needs an empty database to exercise the one-shot setup flow, so it's
    `@DirtiesContext(classMode = AFTER_CLASS)` to force its own Testcontainers instance rather than
    sharing (and racing on) one with the other test classes. `PersonControllerTest`/`TicketControllerTest`
    carry the same annotation for the same reason, and use `@TestInstance(PER_CLASS)` + `@BeforeAll` to
    bootstrap an agent through the real `/api/setup` endpoint once per class before their `@Test` methods
    authenticate as it.
- Each starter has a matching `-test` artifact pulled in (actuator, data-jpa, flyway, hateoas, quartz,
  security, validation, webmvc) — these bring in the respective test-support/autoconfigure test slices
  (e.g. `@DataJpaTest`, `@WebMvcTest`-style testing support) for that starter.
- `architecture.ArchitectureTest` (ArchUnit, `@AnalyzeClasses` over the whole `dev.alkolhar.servdesk`
  package) freezes the layering invariants described above as executable rules rather than just
  prose: feature packages stay free of cycles, controllers never depend on a `*Repository` directly,
  and `*CommandService`/`*QueryService` classes never depend on `org.springframework.web..`. A rule
  for "controllers must never reference an `@Entity` type" was considered and dropped: ArchUnit's
  bytecode-level dependency resolution flags the transient `assembler.toModel(queryService.findById(id))`
  call chain as a dependency on the entity, which would fail on exactly the pattern this codebase
  recommends rather than the one it forbids.

## Agent skills

### Issue tracker

Issues live in GitHub Issues (github.com/alkolhar/servdesk), using the `gh` CLI. See `docs/agents/issue-tracker.md`.

### Triage labels

Default five-role vocabulary (`needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`). See `docs/agents/triage-labels.md`.

### Domain docs

Single-context — one `CONTEXT.md` + `docs/adr/` at the repo root. See `docs/agents/domain.md`.
