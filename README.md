# servdesk

Ein schlankes ITSM-Ticketsystem:
ein einzelner `Person`-Typ für Agents und Customers, ein einzelner `Ticket`-Typ für
Incidents/Service-Requests/Problems/Changes und einer eigenen Workflow-Engine. Schwerere Bausteine (CMDB, SLA/OLA, Knowledge Base,
Workflow-Engine) kommen erst hinzu, wenn ein späterer Ausbauschritt sie tatsächlich braucht.

## Architektur

Modularer Monolith, Feature-Pakete unter `dev.alkolhar.servdesk`:

- **`directory`** — Personen (Agents/Customers) und Teams
- **`classification`** — Kategorien und Prioritäten
- **`ticket`** — der Ticketing-Kern
- **`setup`** — einmaliger Erstanlage-Endpunkt für den ersten Agent-Account
- **`common`** — geteilte Bausteine: `BaseEntity` (Auditing, Optimistic Locking, Soft Delete),
  Domain Events (`common.event`), HTTP-agnostische Exceptions (`common.exception`), RFC-7807-Fehlerbehandlung
  (`common.web`)
- **`config`** — Security, API-Versionierung, JPA-Auditing

Jedes Feature-Paket trennt Entity (persistenzonly) / Request-DTOs (Validierung) / Response-Model
(HATEOAS, nie die Entity selbst) und — seit der CQRS-light-Einführung — Command-Service
(create/update/delete, publiziert Domain Events) von Query-Service (reine Lesezugriffe, inkl.
Pagination/Filtering). Autorisierung ist zentral in `SecurityConfig` geregelt (RBAC über
`ROLE_AGENT`/`ROLE_CUSTOMER`), nicht in den Services verstreut.

Der vollständige Architektur-Kontext (jede Design-Entscheidung mit Begründung, inkl. der
JDK/Tooling-Kompatibilitätsfragen, die während der Modernisierung aufgetreten sind) steht in
[`CLAUDE.md`](CLAUDE.md) — dieses README ist der schnelle Einstieg, CLAUDE.md die Tiefenreferenz.

**API-Dokumentation**: der handgeschriebene OpenAPI-3.0-Contract liegt unter
`src/main/resources/static/openapi/servdesk-api.yaml` und ist über eine Swagger-UI-Ansicht unter
`/docs/index.html` erreichbar, sobald die Anwendung läuft.

## Voraussetzungen für die lokale Entwicklung

- **Docker** — sowohl für Testcontainers (Integrationstests laufen gegen eine echte PostgreSQL) als
  auch für `docker compose`. Kein System-Maven und keine lokale JDK-Installation nötig, wenn nur
  über Docker Compose gearbeitet wird; für IDE-Nutzung wird **JDK 25** empfohlen (der
  Maven-Wrapper `./mvnw` bootstrapt Maven selbst).

## Startanleitung

### Option A: Docker Compose (kompletter Stack, keine lokale JDK nötig)

```bash
docker compose up --build
```

Startet PostgreSQL + die Anwendung; erreichbar unter `http://localhost:8080`. Ersten Agent-Account
anlegen:

```bash
curl -X POST http://localhost:8080/api/setup \
  -H "Content-Type: application/json" \
  -d '{"name":"Admin","email":"admin@example.com","username":"admin","password":"admin123"}'
```

### Option B: lokal mit Maven-Wrapper (eigene PostgreSQL-Instanz nötig)

```bash
./mvnw spring-boot:run
```

Erwartet eine erreichbare PostgreSQL (Verbindungsdaten in `application.properties` bzw. per
`--spring.datasource.*`-Argumenten überschreibbar). Für einen schnellen Einwegcontainer:

```bash
docker run -d --name servdesk-db -p 5432:5432 \
  -e POSTGRES_DB=servdesk -e POSTGRES_USER=servdesk -e POSTGRES_PASSWORD=servdesk \
  postgres:latest
```

### Option C: `TestServdeskApplication`

Ein Dev-Einstiegspunkt, der die Anwendung direkt mit einer Testcontainers-verwalteten,
Wegwerf-PostgreSQL startet — kein manuelles Datenbank-Setup nötig, ideal für schnelles lokales
Ausprobieren aus der IDE heraus.

## Testausführung

Zwei bewusst getrennte Ebenen (siehe `CLAUDE.md` → Testing für die volle Begründung):

```bash
./mvnw test                          # alles: Unit- (Mockito, keine DB) + Integrationstests (Testcontainers/PostgreSQL)
./mvnw test -Dtest=ClassName          # eine Testklasse
./mvnw test -Dtest=ClassName#method   # eine einzelne Testmethode
```

Integrationstests (`*ControllerTest`) brauchen einen laufenden Docker-Daemon. Unit-Tests
(`*CommandServiceTest`, `*QueryServiceTest`) laufen ohne Docker/Datenbank in Bruchteilen von
Sekunden.

## Build-Prozess

```bash
./mvnw compile                          # nur kompilieren
./mvnw verify                           # Standard-Qualitätsgate: Tests, Spotless-Formatcheck, JaCoCo-Report
./mvnw package                          # ausführbares Jar bauen (target/servdesk-*.jar)
./mvnw spotless:apply                   # Formatierung automatisch korrigieren
./mvnw spotbugs:check                   # Static Analysis manuell (nicht in verify gebunden, siehe pom.xml)
./mvnw org.owasp:dependency-check-maven:check   # CVE-Scan manuell (braucht Netzwerkzugriff auf die NVD-Feeds)
docker build -t servdesk .               # Container-Image bauen (Multi-Stage, gelayerter Boot-Jar)
```

`./mvnw verify` ist das, was auch die CI-Pipeline (`.github/workflows/ci.yml`) auf jeden Push/PR
gegen `main` ausführt, zusammen mit einem reinen `docker build`-Validierungsjob.

## Technische Entscheidungen (Kurzfassung)

Die folgenden Entscheidungen sind in `CLAUDE.md` im Detail begründet — hier nur die Kurzfassung:

- **Java 25 / Spring Boot 4.1 / Spring Framework 7**, Maven (kein Gradle).
- **CQRS-light**: Command-/Query-Service-Trennung ohne separates Lesemodell — vorbereitet für den
  Tag, an dem sich das lohnt, ohne es vorzeitig zu erzwingen.
- **Domain Events** über Spring's eingebauten `ApplicationEventPublisher` (kein externer Event-Bus)
  — Hook-Punkt für zukünftige SLA-/Notification-Module.
- **Soft Delete** via Hibernate `@SQLDelete`/`@SQLRestriction` statt echtem `DELETE` — Unique-Spalten
  gelöschter Zeilen bleiben dank partieller Unique-Indizes (`WHERE deleted_at IS NULL`)
  wiederverwendbar.
- **PostgreSQL exklusiv** — die Datenbank ist Teil des Produkts, kein austauschbarer Baustein
  (siehe [ADR-0002](docs/adr/0002-postgresql-only-product-owns-its-database.md)).
- **RFC 7807** (`ProblemDetail`) für alle Fehlerantworten statt eines eigenen Fehlerformats.
- **API-Versionierung** über Header (`X-API-Version`), nicht über URI-Präfixe — bestehende
  Client-URLs bleiben stabil.
- **RBAC** zentral in `SecurityConfig`, nicht über `@PreAuthorize` in den Services verstreut; OAuth2/OIDC
  ist vorbereitet (Dependency vorhanden, Migrationspfad dokumentiert), aber nicht aktiviert, solange
  kein echter Identity Provider zur Verifikation existiert.
- **Contract-first OpenAPI**, handgeschrieben, kein Codegen in beide Richtungen.
- Durchgängig **JSpecify**-Null-Safety-Annotationen (aktuell Dokumentations-/IDE-Ebene, kein
  erzwingendes Tool wie NullAway).
