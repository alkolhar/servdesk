---
status: accepted
---

# The UI is an Angular SPA over the public API, built into the same jar

servdesk had no UI at all. The first one is an **Agent workbench** (customer-facing screens come
later), and it is an **Angular single-page application that is a client of the public REST API**:
every screen calls `/api/**`, the same contract-first, contract-tested, HAL-based API any other
client uses. There is no second server-side web layer. The Angular code lives in `frontend/` in
this repository and is built by Maven into the same Spring Boot jar, so the product stays one
artifact and one deployment.

## Considered options

- **Server-rendered Thymeleaf + htmx** (what the original analysis in #32 leaned toward), rejected.
  It means new `@Controller`s that render HTML and call the command/query services directly, next
  to the REST controllers: two web layers over the same services, with validation and error
  handling duplicated, none of the UI's behaviour covered by the contract tests, the HATEOAS work
  unused, and a session + CSRF login regime that the stateless API deliberately doesn't have. Its
  main argument, "one artifact", doesn't actually separate the options: an SPA's build output is
  packaged into the jar as static resources, exactly as the Swagger UI webjar already is. What it
  genuinely saves is a JavaScript toolchain.
- **React + TypeScript / Vue / Svelte**, rejected in favour of Angular. React has the largest
  ecosystem, but Angular includes the router, forms, HTTP client and dependency injection, and it
  is the most familiar structure to a Spring developer. The maintainer prefers having those
  structural choices made.
- **Building the frontend outside Maven** (its own CI job and Dockerfile stage) or **in a separate
  repository**, rejected. The first gives two build entry points to keep in step (`./mvnw package`
  alone would no longer produce a complete app); the second breaks "one artifact, one version" and
  turns every API-plus-screen change into two PRs.
- **Generated Angular services** from the spec, rejected in favour of generated *models* with
  hand-written services, because generated services build URLs from ids for every operation, which
  works against using links as action affordances (below).
- **UI-side permission logic** (hiding buttons from the UI's own knowledge of role and state),
  rejected. It would duplicate RBAC, row-level ownership, the last-Agent invariant and, with
  Flowable, the process's own idea of which transitions are possible now, and the copy would
  drift.
- **PrimeNG** and **Tailwind + a headless kit**, rejected in favour of Angular Material. PrimeNG's
  table is richer, but its major releases have been disruptive and it doesn't release in lockstep
  with Angular; a headless kit leaves every component for a solo maintainer to build.
- **Angular's built-in compile-time i18n**, rejected. It produces one bundle per language, which
  works against shipping a single bundle in the jar.

## Decision

- **Build.** `frontend/` holds the Angular app. `frontend-maven-plugin` downloads its own Node (no
  system Node required), runs the Angular build, and copies the output into the jar's static
  resources, bound to **`prepare-package`**: `./mvnw package` and the Dockerfile produce the
  complete jar unchanged, while `./mvnw compile`/`test` stay Java-only. For local development,
  `ng serve` with a proxy to the running app on `:8080` gives hot reload.
- **Serving.** The UI is served at `/`. Unknown paths outside `/api`, `/docs`, `/openapi`,
  `/actuator` and `/error` are forwarded to `index.html`, so deep links work. The static UI assets
  are public; how the SPA authenticates against `/api/**` is a separate decision (*How does the
  browser log in?*, #88).
- **Types.** TypeScript models are **generated from `servdesk-api.yaml`** at build time (gitignored,
  never hand-edited), so a spec change that breaks a screen fails `ng build`. HTTP services are
  hand-written. This doesn't conflict with the contract-first "no codegen" rule, which is about
  the server: controllers are still written to match the spec.
- **Hypermedia.** Navigation uses plain URLs. **Actions use links as affordances**: the server adds
  an action link (e.g. a lifecycle transition, completing a task, delete) only when *this caller*
  may perform it *in this state*, and the UI shows the control only when the link is present and
  sends the request to its `href`. The server stays the single judge of role and state.
- **Components.** Angular Material + CDK, which upgrade in lockstep with Angular. Queue filtering
  stays server-side (the API already filters by status, assignee, team, category, priority and
  custom fields), so the UI needs filter controls, not a client-side grid.
- **State.** Services and Angular signals; no NgRx until something demonstrably needs it.
- **Languages.** Runtime translation with **Transloco**, one bundle, **German and English** from
  the first screen. Language comes from the browser locale, with English as the fallback. Every
  user-visible string is externalised. A deployment can add or override translation files, in
  line with ADR-0002's configuration-only customisation.
- **Testing.** Unit and component tests with the Angular CLI's default runner, plus lint, in a
  `frontend` CI job. A Playwright smoke suite (log in, create a ticket, work it through its
  lifecycle), started against the built jar the same way the contract-tests job starts the app,
  is **planned** for once that flow exists. The MVP isn't done without it.

## Consequences

- The API has to serve everything the UI needs; there is no side door. A screen that needs data or
  an operation the API lacks means an API change first, spec included.
- **New server work: action links.** Assemblers must emit caller- and state-aware action links,
  and the spec must document their relations. Today only `self` and navigation relations exist.
  With a Flowable-driven lifecycle, the available transitions and tasks surface this way.
- The build gains a Node toolchain (managed by Maven, not installed system-wide), and
  `./mvnw package` gets slower by the Angular build. The Java test loop does not.
- A second language (TypeScript) and a frontend dependency tree to keep current: Angular and
  Angular Material major upgrades, via `ng update`.
- The contract tests keep covering exactly the surface the UI uses, because the UI has no other
  surface.
