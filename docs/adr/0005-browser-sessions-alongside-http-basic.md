---
status: accepted
---

# Browser sessions alongside HTTP Basic

Until now, the API was **stateless HTTP Basic on every request, with CSRF disabled**: no cookies
and no sessions, and CSRF's filter (which runs before Basic authentication) would only have
rejected valid writes. The first UI is an Angular SPA served from the same jar and calling
`/api/**` (ADR-0003), and it needs a login. The browser now logs in to a **server-side session
held in an `HttpOnly` cookie**, while **HTTP Basic stays** for scripts, CI and the contract tests.
Because a cookie now authenticates requests, **CSRF protection comes back for session-authenticated
requests**.

## Considered options

- **The SPA keeps sending HTTP Basic credentials.** Rejected: the SPA would have to keep the
  plain password. In memory, every page reload logs you out; in `sessionStorage`, any XSS bug can
  read the password itself. There is no real logout and no server-side timeout.
- **Tokens issued by servdesk** (JWT access + refresh). Rejected: signing keys, refresh and
  revocation, all for a single same-origin client. When OIDC (#48) arrives, an IdP issues the
  tokens and this would be thrown away. Current guidance for browser apps favours a server-side
  session, the backend-for-frontend pattern, over tokens held in JavaScript.
- **Spring Session JDBC** (sessions in Postgres). Deferred, not rejected: servdesk runs as one
  instance per customer (ADR-0002), so losing sessions on restart is an annoyance, not a
  failure. Moving to it later is configuration plus a migration, invisible to the SPA.
- **Hard account lockout** after repeated failures. Rejected: anyone who knows a username could
  lock that person out on purpose, including the last admin.

## Decision

- **Login and logout.** `POST /api/login` takes JSON credentials and establishes the session;
  `POST /api/logout` invalidates it and clears the cookie. Session-fixation protection is on
  (Spring's default: a new session id on login). `GET /api/me` returns the logged-in person (name,
  role, `admin` flag, teams, and their action links); the SPA uses it to start up, and a `401` on
  it sends the SPA to its login screen.
- **Two ways to authenticate on `/api/**`**: the session cookie, or an `Authorization: Basic`
  header. CI, the contract tests and scripts keep working unchanged.
- **CSRF.** On for session-authenticated requests, using Spring's cookie-based CSRF token
  (`XSRF-TOKEN` cookie, `X-XSRF-TOKEN` header), which Angular's `HttpClient` sends automatically
  for same-origin requests. Requests authenticated by Basic carry no session cookie and stay
  exempt.
- **Cookie.** `HttpOnly`, `SameSite=Strict` (the SPA is same-origin and `index.html` is public),
  and `Secure` wherever the deployment serves HTTPS. That is a deployment setting, so plain-HTTP
  `localhost` development still works.
- **Sessions in memory**, with an **8-hour idle timeout**, configurable
  (`server.servlet.session.timeout`), and no "remember me".
- **Brute-force throttling in memory**, per username and per client IP, covering both
  `/api/login` and failed Basic authentication. After 5 failures in a row it answers `429` with
  `Retry-After` and a growing backoff (for example 1, 5, 15 minutes); a success resets the count.
  The numbers are configurable defaults. No account lockout. Behind a reverse proxy, the client IP
  is only meaningful if the proxy's forwarded header is trusted, and that is a deployment setting.
- **Changes that must take effect immediately end live sessions.** Deactivating a person, an
  admin password reset, and removing a person's `admin` flag invalidate that person's sessions.
  Otherwise the session's cached authorities would keep a deactivated Agent logged in, or a
  removed admin empowered, for up to the idle timeout.
- The `401`/`403`/`429` answers stay `ProblemDetail`, and the `401` still carries **no
  `WWW-Authenticate: Basic`** header, so the browser never shows its native login dialog over the
  SPA.

## Consequences

- `CLAUDE.md`'s `SecurityConfig` section ("CSRF disabled — HTTP Basic on every request, no
  cookies/sessions") is superseded and must be rewritten when this is built.
- The contract gains `/api/login`, `/api/logout` and `/api/me`, plus a `429` response on
  authenticated operations.
- A restart or redeploy logs everyone out, which is accepted for the MVP.
- **OIDC stays easy:** `oauth2Login` would establish the same kind of session, so the SPA doesn't
  change; only how the session starts does. The resource-server path for bearer tokens is
  unaffected.
- Throttling state and sessions both live in one JVM's memory. A multi-instance deployment would
  need both moved to shared storage.
