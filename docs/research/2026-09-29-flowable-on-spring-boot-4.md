# Flowable on Spring Boot 4.1, Spring Framework 7 and Java 25

Research for [#82](https://github.com/alkolhar/servdesk/issues/82) (child of the
[#81](https://github.com/alkolhar/servdesk/issues/81) map). Captured 2026-09-29.

**Scope of trust.** Every claim below is sourced to Maven Central metadata and POMs, the
`flowable/flowable-engine` repository at tag `flowable-8.0.0` (and `main` where noted), its GitHub
release notes, or Flowable's own open-source documentation. No blogs, no Stack Overflow. One section is
backed by an experiment instead of a document; it is labelled as such. Where a question could *not* be
settled from a primary source it is called out under "Not established".

**What the repo runs today** (`pom.xml`): `spring-boot-starter-parent` **4.1.0**, which manages Spring
Framework **7.0.8**, Hibernate **7.4.1.Final**, Jackson **3.1.4**, Flyway **12.4.0**, Spring Security
**7.1.0**; `java.version` **25**; PostgreSQL `postgres:latest` in compose, CI and Testcontainers.
Source: [`spring-boot-dependencies-4.1.0.pom`](https://repo1.maven.org/maven2/org/springframework/boot/spring-boot-dependencies/4.1.0/spring-boot-dependencies-4.1.0.pom).

---

## Answer in one paragraph

**Yes, with one qualification.** The only Flowable line for Boot 4 is **8.x**, and the only 8.x release is
**`8.0.0` (2026-02-27)**. It is *built against Spring Boot 4.0.2 / Spring Framework 7.0.3*, not Boot 4.1.
Boot 4.1 support exists on `main` (8.1.0-SNAPSHOT, merged 2026-05-06 as PR #4209) but is **unreleased**.
The gap between the two is small and concrete. Framework stays on the 7.0.x line, and the 4.1 upgrade
commit changed no code in the engine, the Spring integration or the Boot auto-configuration. Its only
source changes are in the Kafka and RabbitMQ event-registry adapters, which servdesk would not use. A
scratch Boot **4.1.0** app on **Java 25** with `flowable-spring-boot-starter-process:8.0.0` booted,
deployed a BPMN process, ran with the IDM engine off, and committed and rolled back together with JPA
under Boot's `JpaTransactionManager` (see §5). **Recommended coordinates:
`org.flowable:flowable-spring-boot-starter-process:8.0.0`**, plus `flowable.idm.enabled=false`.

---

## 1. Latest release and what it is built against

**Releases.** Maven Central lists `8.0.0` as `<latest>`/`<release>` for both `flowable-engine` and
`flowable-spring-boot-starter-process`. The full 7.x/8.x sequence is 7.0.0, 7.0.1, 7.1.0, 7.2.0,
8.0.0. There is no 8.0.x patch and no 8.1 milestone. The last metadata update was 2026-02-27.
Sources: [`flowable-engine/maven-metadata.xml`](https://repo1.maven.org/maven2/org/flowable/flowable-engine/maven-metadata.xml),
[`flowable-spring-boot-starter-process/maven-metadata.xml`](https://repo1.maven.org/maven2/org/flowable/flowable-spring-boot-starter-process/maven-metadata.xml);
GitHub API `repos/flowable/flowable-engine/releases` (`flowable-8.0.0`, published 2026-02-27T16:35:48Z).

**Versions it is built against.** The POM chain is `flowable-root` → modules → `flowable-parent` →
`flowable-dependencies` → `flowable-bom`. The Spring versions are set in `flowable-dependencies`:

| Property in `flowable-dependencies-8.0.0.pom` | 8.0.0 (released) | `main` / 8.1.0-SNAPSHOT | servdesk (Boot 4.1.0 BOM) |
| --- | --- | --- | --- |
| `spring.boot.version` | **4.0.2** | 4.1.0-RC1 | 4.1.0 |
| `spring.framework.version` | **7.0.3** | 7.0.7 | 7.0.8 |
| `spring.security.version` | 7.0.2 | 7.1.0-RC1 | 7.1.0 |
| `jackson3.version` | 3.0.1 | (bumped) | 3.1.4 |
| `hibernate.version` (test scope only) | 7.1.8.Final | | 7.4.1.Final |
| `mybatis.version` (Flowable's own persistence) | 3.5.19 | | not managed by Boot |
| `jdk.version` (compile `--release`) | **17** | 17 | 25 |

Sources: [`flowable-dependencies-8.0.0.pom`](https://repo1.maven.org/maven2/org/flowable/flowable-dependencies/8.0.0/flowable-dependencies-8.0.0.pom),
[`flowable-parent-8.0.0.pom`](https://repo1.maven.org/maven2/org/flowable/flowable-parent/8.0.0/flowable-parent-8.0.0.pom)
(`<jdk.version>17</jdk.version>`, fed to `maven-compiler-plugin` `source`/`target`/`release`),
[`modules/flowable-dependencies/pom.xml` @main](https://github.com/flowable/flowable-engine/blob/main/modules/flowable-dependencies/pom.xml).

**Boot 4 support status.** The 8.0.0 release notes open with: "**Upgrade to Spring Framework 7 and
Spring Boot 4** — Flowable 8 is now based on Spring Framework 7 and Spring Boot 4. There is no support
for Spring Boot 3 anymore." They also list **Jackson 3** as the default JSON library, with Jackson 2
still available via `flowable.variable-json-mapper=jackson2`.
Source: [release `flowable-8.0.0`](https://github.com/flowable/flowable-engine/releases/tag/flowable-8.0.0).
The Spring Boot chapter of the docs states "Flowable supports Spring Boot 4.x." but names no minor.
Source: [Flowable docs → Spring Boot](https://www.flowable.com/open-source/docs/bpmn/ch05a-Spring-Boot).

**Java baseline.** Flowable 8 compiles with `--release 17`. At the 8.0.0 tag its main CI workflow
builds and tests on a JDK matrix of **`[17, 21, 25]`**. Java 25 was added in commit `a0b909cdc4`
("Add Java 25 build step", 2025-12-24). That commit also bumped Artemis to a version that supports
Java 25. Sources: [`.github/workflows/main.yml` @flowable-8.0.0](https://github.com/flowable/flowable-engine/blob/flowable-8.0.0/.github/workflows/main.yml),
[commit a0b909cdc4](https://github.com/flowable/flowable-engine/commit/a0b909cdc4).

### The 4.0 → 4.1 gap, concretely

The commit that moved `main` to Boot 4.1 is
[`53f9d701ff` "Upgrade to Spring Boot 4.1.0-RC1"](https://github.com/flowable/flowable-engine/commit/53f9d701ff)
(PR [#4209](https://github.com/flowable/flowable-engine/pull/4209), 2026-05-06). Its whole diff:

- `flowable-dependencies/pom.xml`: Spring Boot/AMQP/Kafka/LDAP/Security → 4.1.0-RC1 line, Framework
  7.0.3 → 7.0.7, Artemis 2.44.0 → 2.53.0.
- `flowable-spring-boot/pom.xml`: removes an `artemis-bom` override whose comment reads "Spring Boot 4.0
  is using Artemis 2.43.0 which is not compatible with java 25".
- **Two source fixes, both in `flowable-event-registry-spring`:**
  `SimpleKafkaListenerEndpoint` gains `getAckMode()` (Spring Kafka 4.1 added it to
  `KafkaListenerEndpoint`), and `RabbitChannelDefinitionProcessor` switches to the core
  `org.springframework.amqp.core.MessageListenerContainer` (Spring AMQP 4.1 deprecated the old one).

The commit changed no file in `flowable-spring-boot-autoconfigure`, `flowable-spring`,
`flowable-spring-common` or the engine. The Kafka and RabbitMQ adapters only come into play when
`spring-kafka` or `spring-rabbit` is on the classpath and an event-registry channel uses them.
servdesk has neither. On a JMS/Artemis setup the Artemis-on-Java-25 issue is a Boot 4.0 problem
that Boot 4.1's own BOM already fixes (Artemis 2.53.0). servdesk doesn't use Artemis either.

**Assessment:** 8.0.0 on Boot 4.1.0 is an *untested-by-upstream but low-risk* pairing for the
BPMN-only use case. All Spring artifacts resolve to Boot 4.1's versions, since Boot's BOM wins in
servdesk's build. Framework stays on 7.0.x. Upstream needed no code change in anything servdesk
would load. §5 shows the pairing actually working. The residual risk is a binary incompatibility in
a code path the smoke test didn't exercise. §5 lists what it didn't cover.

---

## 2. Minimal embedded setup — BPMN engine only, no REST/IDM/UI

**Starters at the 8.0.0 tag.** Directory `modules/flowable-spring-boot/flowable-spring-boot-starters`:
`-autoconfigure`, `-starter` (all engines), `-starter-process`, `-starter-cmmn`, `-starter-dmn`,
`-starter-app`, the matching `-rest` variants, `-starter-actuator` and `-starter-integration`.
Source: [directory listing @flowable-8.0.0](https://github.com/flowable/flowable-engine/tree/flowable-8.0.0/modules/flowable-spring-boot/flowable-spring-boot-starters).
There is no separate UI app artifact in this repository. The REST APIs are opt-in via the `*-rest`
starters.

The docs describe the two relevant starters as "`flowable-spring-boot-starter`: Contains dependencies
for booting all Flowable Engines (Process, CMMN, DMN, and IDM)" and "`flowable-spring-boot-starter-process`:
Contains dependencies for booting the Process Engine in Standalone mode".
Source: [Flowable docs → Spring Boot](https://www.flowable.com/open-source/docs/bpmn/ch05a-Spring-Boot).

**Recommended coordinate:**

```xml
<dependency>
    <groupId>org.flowable</groupId>
    <artifactId>flowable-spring-boot-starter-process</artifactId>
    <version>8.0.0</version>
</dependency>
```

Its POM depends on exactly `flowable-spring-boot-autoconfigure`, `flowable-engine`, `flowable-spring` and
`spring-boot-starter-jdbc`. Source: [`flowable-spring-boot-starter-process-8.0.0.pom`](https://repo1.maven.org/maven2/org/flowable/flowable-spring-boot-starter-process/8.0.0/flowable-spring-boot-starter-process-8.0.0.pom).
In `flowable-spring-boot-autoconfigure`, every REST module (`flowable-rest`, `-cmmn-rest`, `-idm-rest`,
...), `spring-boot-starter-web`, `-data-jpa` and `-security` are declared `<optional>true</optional>`.
The one exception is **`flowable-spring-security`**, which is a hard compile dependency (a
`TODO make it optional` comment sits next to it).
Source: [`flowable-spring-boot-autoconfigure-8.0.0.pom`](https://repo1.maven.org/maven2/org/flowable/flowable-spring-boot-autoconfigure/8.0.0/flowable-spring-boot-autoconfigure-8.0.0.pom).

**Resolved tree** (`./mvnw dependency:tree` on a scratch project with parent Boot 4.1.0 plus
`spring-boot-starter-data-jpa` and the process starter, run outside this repo): no Flowable REST
module, no CMMN/DMN *engine* (only their `-api`/`-model` jars), no Liquibase. MyBatis 3.5.19,
`tools.jackson.core:jackson-databind` resolves to **3.1.4** (Boot's), and every
`org.springframework:*` resolves to **7.0.8** (Boot's).

**Can IDM be excluded entirely? Its *engine* can be switched off. Its *jars* cannot be dropped by
the starter alone.**

- `flowable-engine` itself depends on `flowable-idm-api`, `flowable-idm-engine` and
  `flowable-idm-engine-configurator` (compile scope), so the classes are always on the classpath.
- Switching it off is one property. `flowable.idm.enabled` is documented as "Whether the idm engine
  needs to be started", default `true`.
  Source: [`FlowableIdmProperties.java` @flowable-8.0.0](https://github.com/flowable/flowable-engine/blob/flowable-8.0.0/modules/flowable-spring-boot/flowable-spring-boot-starters/flowable-spring-boot-autoconfigure/src/main/java/org/flowable/spring/boot/idm/FlowableIdmProperties.java).
  `IdmEngineAutoConfiguration` is guarded by `@ConditionalOnIdmEngine`, which requires both
  `flowable.db-identity-used` and `flowable.idm.enabled` to be `true` (`matchIfMissing = true`).
  `ProcessEngineAutoConfiguration` sets
  `conf.setDisableIdmEngine(!(flowableProperties.isDbIdentityUsed() && idmProperties.isEnabled()))`.
  Sources: [`ConditionalOnIdmEngine.java`](https://github.com/flowable/flowable-engine/blob/flowable-8.0.0/modules/flowable-spring-boot/flowable-spring-boot-starters/flowable-spring-boot-autoconfigure/src/main/java/org/flowable/spring/boot/condition/ConditionalOnIdmEngine.java),
  [`ProcessEngineAutoConfiguration.java` L233](https://github.com/flowable/flowable-engine/blob/flowable-8.0.0/modules/flowable-spring-boot/flowable-spring-boot-starters/flowable-spring-boot-autoconfigure/src/main/java/org/flowable/spring/boot/ProcessEngineAutoConfiguration.java#L233).
- With it off, the experiment in §5 showed zero `IdmEngine` beans, `isDisableIdmEngine() == true`, and
  **no identity DDL executed**, so no `ACT_ID_*` tables.
- Excluding the three IDM jars with Maven `<exclusions>` was **not tested**. The engine code
  references IDM types, so treat that as unsupported (see "Not established").

**Two interactions with servdesk's own security, both from source:**

1. **`PasswordEncoder` fallback.** When IDM is enabled, `IdmEngineAutoConfiguration.PasswordEncoderConfiguration`
   registers a `@ConditionalOnMissingBean PasswordEncoder` that defaults to **`NoOpPasswordEncoder`**
   unless `flowable.idm.password-encoder` says otherwise. servdesk defines its own `PasswordEncoder`
   bean in `SecurityConfig`, so the fallback backs off either way, and with `flowable.idm.enabled=false`
   the configuration class isn't loaded at all. This matters if someone later removes servdesk's
   bean while IDM is on.
   Source: [`IdmEngineAutoConfiguration.java` @flowable-8.0.0](https://github.com/flowable/flowable-engine/blob/flowable-8.0.0/modules/flowable-spring-boot/flowable-spring-boot-starters/flowable-spring-boot-autoconfigure/src/main/java/org/flowable/spring/boot/idm/IdmEngineAutoConfiguration.java).
2. **No automatic Spring Security → Flowable user bridge without IDM.** `FlowableSecurityAutoConfiguration`
   is `@ConditionalOnBean(IdmIdentityService.class)`. It both installs `SpringSecurityAuthenticationContext`
   (so Flowable's `Authentication.getAuthenticatedUserId()` reads the `SecurityContextHolder`) and
   registers a `FlowableUserDetailsService` only if no `UserDetailsService` exists. With IDM off,
   *neither* happens. servdesk's `PersonUserDetailsService` stays the only `UserDetailsService`, which
   is good. The flip side is that Flowable won't know the current user by itself, e.g. for a process
   initiator. A later ticket has to set it explicitly: `Authentication.setAuthenticationContext(new SpringSecurityAuthenticationContext())`
   from `flowable-spring-security` (already on the classpath), or pass user ids explicitly.
   Source: [`FlowableSecurityAutoConfiguration.java` @flowable-8.0.0](https://github.com/flowable/flowable-engine/blob/flowable-8.0.0/modules/flowable-spring-boot/flowable-spring-boot-starters/flowable-spring-boot-autoconfigure/src/main/java/org/flowable/spring/boot/FlowableSecurityAutoConfiguration.java).

Task assignment itself needs no IDM. `assignee` and candidate users/groups are plain strings in
`ACT_RU_TASK`/`ACT_RU_IDENTITYLINK`, which live in the *common* schema (§3), not in `ACT_ID_*`.

---

## 3. Schema creation and upgrade on PostgreSQL

Flowable persists through **MyBatis, not Hibernate**, and manages its own DDL. Hibernate's
`ddl-auto=validate` only checks servdesk's `@Entity` mappings and ignores `ACT_*`/`FLW_*` tables
entirely. The collision is with **Flyway's** "only schema source of truth" rule.

### `databaseSchemaUpdate` modes

Documented values: `false` (default at engine level), `true`, `create-drop`.

- **`false`**: "Checks the version of the DB schema against the library when the process engine is
  being created and throws an exception if the versions don't match."
- **`true`**: "Upon building the process engine, a check is performed and an update of the schema is
  performed if it is necessary. If the schema doesn't exist, it is created."
- **`create-drop`**: "Creates the schema when the process engine is being created and drops the
  schema when the process engine is being closed."

Source: [Flowable docs → Configuration → Database configuration](https://www.flowable.com/open-source/docs/bpmn/ch03-Configuration).

The source defines five constants, the documented three plus `create` and `drop-create`, in
`AbstractEngineConfiguration`
([L144-L156 @flowable-8.0.0](https://github.com/flowable/flowable-engine/blob/flowable-8.0.0/modules/flowable-engine-common/src/main/java/org/flowable/common/engine/impl/AbstractEngineConfiguration.java#L144-L156)).
They are dispatched in `SchemaOperationsEngineBuild.executeSchemaUpdate`: `false` → `schemaCheckVersion`,
`true` → `schemaUpdate`, `create`/`create-drop`/`drop-create` → `schemaCreate`
([source](https://github.com/flowable/flowable-engine/blob/flowable-8.0.0/modules/flowable-engine-common/src/main/java/org/flowable/common/engine/impl/db/SchemaOperationsEngineBuild.java)).
Any other string matches no branch and does nothing. That follows from the code but is **not a
documented mode** (see "Not established").

**The Spring Boot default is `true`, not `false`.** `FlowableProperties.databaseSchemaUpdate = "true"`
("The strategy that should be used for the database schema"), bound as
`flowable.database-schema-update`. So **out of the box the starter auto-creates and auto-upgrades its
tables at startup, outside Flyway.**
Sources: [`FlowableProperties.java` L64 @flowable-8.0.0](https://github.com/flowable/flowable-engine/blob/flowable-8.0.0/modules/flowable-spring-boot/flowable-spring-boot-starters/flowable-spring-boot-autoconfigure/src/main/java/org/flowable/spring/boot/FlowableProperties.java#L64);
docs property list ("`flowable.database-schema-update=true`"), [Spring Boot chapter](https://www.flowable.com/open-source/docs/bpmn/ch05a-Spring-Boot).
A related property, `flowable.use-lock-for-database-schema-update` (default `false`), makes concurrent
nodes serialise schema changes.

Experiment (§5): with `flowable.database-schema-update=false` on an empty database, startup fails with
`FlowableException: no flowable tables in db. set <property name="databaseSchemaUpdate" to value="true" ...`,
caused by `ACT_GE_PROPERTY` not found. So `false` is a genuine fail-fast check, the Flowable
counterpart of `ddl-auto=validate`.

### Where the DDL lives in the jars

Script naming is documented as `flowable.{db}.{create|drop}.{type}.sql`, with create scripts under
`org/flowable/db/create`. Upgrade scripts are applied by the engine under `databaseSchemaUpdate=true`,
or can be run manually. Source: [Configuration chapter](https://www.flowable.com/open-source/docs/bpmn/ch03-Configuration).

In 8.0.0 the DDL is split across **several jars**, one per component. Listed from the resolved jars
of the process starter, PostgreSQL only:

| Jar | Create script | Tables | Version row it inserts into `ACT_GE_PROPERTY` |
| --- | --- | --- | --- |
| `flowable-engine-common` | `org/flowable/common/db/create/flowable.postgres.create.common.sql` | `ACT_GE_PROPERTY`, `ACT_GE_BYTEARRAY`, `ACT_RU_TASK`, `ACT_RU_VARIABLE`, `ACT_RU_IDENTITYLINK`, `ACT_RU_ENTITYLINK`, `ACT_RU_EVENT_SUBSCR`, `ACT_RU_*JOB` (job/timer/suspended/deadletter/history/external), `FLW_RU_BATCH(_PART)`, `ACT_HI_TASKINST`, `ACT_HI_TSK_LOG`, `ACT_HI_VARINST`, `ACT_HI_IDENTITYLINK`, `ACT_HI_ENTITYLINK` | `common.schema.version = 8.0.0.0`, `next.dbid` |
| `flowable-engine` | `org/flowable/db/create/flowable.postgres.create.engine.sql` | `ACT_RE_DEPLOYMENT`, `ACT_RE_PROCDEF`, `ACT_RE_MODEL`, `ACT_RU_EXECUTION`, `ACT_RU_ACTINST`, `ACT_PROCDEF_INFO`, `ACT_EVT_LOG` | `schema.version = 8.0.0.0`, `schema.history` |
| `flowable-engine` | `org/flowable/db/create/flowable.postgres.create.history.sql` | `ACT_HI_PROCINST`, `ACT_HI_ACTINST`, `ACT_HI_DETAIL`, `ACT_HI_COMMENT`, `ACT_HI_ATTACHMENT` | none |
| `flowable-event-registry` | `org/flowable/eventregistry/db/create/flowable.postgres.create.eventregistry.sql` | `FLW_EVENT_DEPLOYMENT`, `FLW_EVENT_RESOURCE`, `FLW_EVENT_DEFINITION`, `FLW_CHANNEL_DEFINITION` | `eventregistry.schema.version = 8.0.0.0` |
| `flowable-idm-engine` (**skipped when IDM is off**) | `org/flowable/idm/db/create/flowable.postgres.create.identity.sql` | `ACT_ID_*` (9 tables) | `schema.version` in `ACT_ID_PROPERTY` |

Matching `drop` scripts sit in sibling `.../db/drop/` directories. **Upgrade scripts** are under
`.../db/upgrade/` named `flowable.postgres.upgradestep.<from>.to.<to>.<component>.sql`. For
example, the step into 8.0 is `upgradestep.7202.to.8000.engine.sql` / `.history.sql`. Counts for
postgres: 85 in `flowable-engine`, 27 in `flowable-engine-common`, 3 in `flowable-event-registry`,
3 in `flowable-idm-engine`. The upgrader walks every version step from the stored version to the current one, running
the `postgres` script and an optional `all` script for each step.
Source: `dbSchemaUpgrade` in [`AbstractSqlScriptBasedDbSchemaManager.java` @flowable-8.0.0](https://github.com/flowable/flowable-engine/blob/flowable-8.0.0/modules/flowable-engine-common/src/main/java/org/flowable/common/engine/impl/db/AbstractSqlScriptBasedDbSchemaManager.java).
The experiment's startup log confirms the create order: `common` → `engine` → `history` → `eventregistry`.

**What this means for the Flyway ticket.** There are two supported shapes, and the decision belongs to
that ticket:

- **Flyway owns it.** Copy the four postgres create scripts into a Flyway migration, and later the
  matching `upgradestep` scripts on each Flowable bump. Then run with
  `flowable.database-schema-update=false`, which fails fast if Flyway and the jar disagree on
  `schema.version`. This is the documented "DBA runs the SQL" path.
- **Flowable owns its own tables** (`true`, the Boot default), with Flyway owning servdesk's tables
  only. That breaks the "Flyway is the only schema source of truth" rule in `CLAUDE.md`, so it is an
  ADR-level exception, not a default to fall into.

Either way, the Flowable tables sit in the same schema as servdesk's by default. Their names
(`act_*`, `flw_*`) don't collide with anything in `V1__init_schema.sql`.

---

## 4. Transactions — one Spring transaction for a ticket write and a process step

**Answer: yes. Flowable's Spring configuration runs every engine command inside Spring's
`PlatformTransactionManager` and joins an active transaction. Under Boot + Data JPA that manager is
the `JpaTransactionManager`, and Flowable's JDBC connection is the same one Hibernate is using.**

The chain, from source at `flowable-8.0.0`:

1. **Boot hands Flowable the context's transaction manager.** `ProcessEngineAutoConfiguration.springProcessEngineConfiguration(DataSource, PlatformTransactionManager, ...)`
   calls `configureSpringEngine(conf, platformTransactionManager)` →
   `engineConfiguration.setTransactionManager(transactionManager)`.
   Sources: [`ProcessEngineAutoConfiguration.java`](https://github.com/flowable/flowable-engine/blob/flowable-8.0.0/modules/flowable-spring-boot/flowable-spring-boot-starters/flowable-spring-boot-autoconfigure/src/main/java/org/flowable/spring/boot/ProcessEngineAutoConfiguration.java),
   [`AbstractSpringEngineAutoConfiguration.java`](https://github.com/flowable/flowable-engine/blob/flowable-8.0.0/modules/flowable-spring-boot/flowable-spring-boot-starters/flowable-spring-boot-autoconfigure/src/main/java/org/flowable/spring/boot/AbstractSpringEngineAutoConfiguration.java).
   The IDM engine configuration takes the same bean when enabled.
2. **Every service call goes through `SpringTransactionInterceptor`.** For propagation `REQUIRED` with
   an actual transaction active, it calls straight through, joining the caller's transaction.
   Otherwise it opens one via a `TransactionTemplate` on that manager.
   Source: [`SpringTransactionInterceptor.java`](https://github.com/flowable/flowable-engine/blob/flowable-8.0.0/modules/flowable-spring-common/src/main/java/org/flowable/common/spring/SpringTransactionInterceptor.java);
   the docs describe it as "an extra interceptor to the services that applies Propagation.REQUIRED
   transaction semantics on the Flowable service methods", and say domain work can be "combined in
   the same transaction as the startProcessInstanceByKey"
   ([Spring chapter](https://www.flowable.com/open-source/docs/bpmn/ch05-Spring)).
3. **Flowable's JDBC goes through a `TransactionAwareDataSourceProxy`.** `SpringProcessEngineConfiguration.setDataSource`
   wraps any plain `DataSource` in one
   ([source](https://github.com/flowable/flowable-engine/blob/flowable-8.0.0/modules/flowable-spring/src/main/java/org/flowable/spring/SpringProcessEngineConfiguration.java)).
   The docs say this is done "to make sure the SQL connections retrieved from the DataSource and the
   Spring transactions play well together" ([Spring chapter](https://www.flowable.com/open-source/docs/bpmn/ch05-Spring)).
   Transaction-lifecycle callbacks (`SpringTransactionContextFactory`) also register on the same manager.
4. **`JpaTransactionManager` shares its connection with such code.** Spring's Javadoc: "This transaction
   manager also supports direct DataSource access within a transaction (i.e. plain JDBC code working
   with the same DataSource) … Application code needs to stick to the same simple Connection lookup
   pattern as with DataSourceTransactionManager (i.e. `DataSourceUtils.getConnection(DataSource)` or
   going through a `TransactionAwareDataSourceProxy`). Note that this requires a vendor-specific
   JpaDialect", and "This transaction manager will autodetect the DataSource used as the connection
   factory of the EntityManagerFactory".
   Source: [`JpaTransactionManager` Javadoc, Spring Framework 7.0.x](https://docs.spring.io/spring-framework/docs/7.0.x/javadoc-api/org/springframework/orm/jpa/JpaTransactionManager.html).
   Boot's Hibernate setup supplies `HibernateJpaDialect` and the EMF's `DataSource`, so step 3's proxy
   gets Hibernate's connection.
5. **Flowable also knows about the JPA context.** `FlowableJpaAutoConfiguration` (active when an
   `EntityManagerFactory` bean exists) sets `jpaEntityManagerFactory`, `jpaHandleTransaction=false` and
   `jpaCloseEntityManager=false`, meaning Flowable leaves the JPA transaction to Spring.
   Source: [`FlowableJpaAutoConfiguration.java`](https://github.com/flowable/flowable-engine/blob/flowable-8.0.0/modules/flowable-spring-boot/flowable-spring-boot-starters/flowable-spring-boot-autoconfigure/src/main/java/org/flowable/spring/boot/FlowableJpaAutoConfiguration.java).
   The docs say the same: "by adding the JPA dependency above, the DataSourceTransactionManager which
   we were using before is now automatically swapped out by a JpaTransactionManager"
   ([Spring Boot chapter](https://www.flowable.com/open-source/docs/bpmn/ch05a-Spring-Boot)).

**Consequence for design:** a `@Transactional` service method that saves a `Ticket` through Spring Data
and then calls `runtimeService.startProcessInstanceByKey(...)` or `taskService.complete(...)` is one
database transaction. An exception after both calls rolls back both (demonstrated in §5). The caveat
is that **async continuations and timer jobs run later on the async executor**, each in its own new
transaction. Only the synchronous part of a process step shares the caller's transaction. That is
standard Flowable behaviour, and it matters if SLA timers move into BPMN (a "not yet specified" item
on #81).

---

## 5. Experiment: Boot 4.1.0 + Java 25 + Flowable 8.0.0

Not a document. A scratch project outside the repo (nothing committed), run on the maintainer's
machine because it needs no Docker. The setup: parent `spring-boot-starter-parent:4.1.0`,
`java.version=25`, `spring-boot-starter-data-jpa`, `spring-boot-starter-security`,
`flowable-spring-boot-starter-process:8.0.0` and H2 in-memory. Properties were
`flowable.idm.enabled=false` and `flowable.async-executor-activate=false`. The app had one JPA entity,
one BPMN process (start → user task → end) auto-deployed from `classpath:processes/`, and a
`@Transactional` method that saves the entity, starts the process and optionally throws.

Output (verbatim):

```
PROBE boot=4.1.0 framework=7.0.8 java=25.0.3+11-LTS flowable=8.0.0.0
PROBE txManager=org.springframework.orm.jpa.JpaTransactionManager
PROBE idmEngineBeans=0 idmDisabled=true
PROBE after commit: things=1 procs=1 tasks=1
PROBE caught boom
PROBE after rollback: things=1 procs=1 tasks=1
```

The counts are unchanged after the failing call, so the JPA row, the process instance and its
user task all rolled back together. Schema creation logged `common`, `engine`, `history` and
`eventregistry` only, with no `identity`. A second run with `flowable.database-schema-update=false`
failed at startup with `no flowable tables in db` (§3).

**What it did not cover:** PostgreSQL (it ran on H2, while the SQL scripts are per-database), the
async executor and timers, Flyway, servdesk's own context (Quartz, Spring Integration, HATEOAS), and
a native or AOT build.

---

## Summary of what changed in the map's assumptions

| Map assumption | Finding |
| --- | --- |
| "Flowable 8 works with Spring Boot 4.1" | **Works, but isn't what it was built for.** 8.0.0 is built on Boot **4.0.2** / Framework 7.0.3. Boot 4.1 is on unreleased `main` (8.1.0-SNAPSHOT). The upgrade diff touches only Kafka/RabbitMQ adapters servdesk won't load, and a Boot 4.1.0 + Java 25 smoke test passed. |
| Java 25 is a risk | **No.** `--release 17` baseline, and upstream CI tests on 17/21/25 at the 8.0.0 tag. |
| IDM can be left out | **Its engine and tables, yes** (`flowable.idm.enabled=false`). **Its jars, no**: `flowable-engine` depends on them. And without IDM, Flowable doesn't bridge Spring Security's current user by itself. |
| Flyway stays the only schema owner | **Not by default.** The Boot starter defaults to `database-schema-update=true` and creates/upgrades its own tables. Flyway ownership needs the DDL copied into migrations plus `false` as a fail-fast check. |
| Ticket write + process step commit together | **Yes**, through `SpringTransactionInterceptor` + `TransactionAwareDataSourceProxy` + `JpaTransactionManager`, shown in the experiment. Async/timer jobs are the exception. |

## Not established

1. **Upstream testing of 8.0.0 on Boot 4.1.** No primary source says 8.0.0 was tested against Boot 4.1.
   The only 4.1-aligned build is unreleased `main`. There is no announced date for 8.1.0 or an
   8.0.x on Boot 4.1: no GitHub milestones, and nothing on Maven Central.
2. **PostgreSQL 18.** servdesk runs `postgres:latest`, which is 18 as of this date. Flowable's
   `postgres.yml` workflow at the 8.0.0 tag tests Postgres **14, 15, 16, 17** only
   ([source](https://github.com/flowable/flowable-engine/blob/flowable-8.0.0/.github/workflows/postgres.yml)).
   The docs name no supported Postgres versions.
3. **Dropping the IDM jars via Maven `<exclusions>`.** Not tested, not documented. Assume unsupported.
4. **A "do nothing" schema mode.** An unrecognised `database-schema-update` value such as `ignore`
   skips all schema work in the 8.0.0 source, but it is not a documented mode. Prefer `false`.
5. **Binary compatibility beyond the smoke test** (async executor, timers, event registry on Boot
   4.1's classes). Established only for the synchronous BPMN + JPA path shown in §5.
6. **Running the real integration suite with Flowable added**, on PostgreSQL via Testcontainers.
   Docker networking is broken on the maintainer's machine, so this is left to CI once a build
   ticket adds the dependency.
