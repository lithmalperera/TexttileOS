# Learning Guide

This guide explains everything the project builds, step by step: what each piece is, why it exists, and what it teaches you. Read it alongside the code. It is updated after every implementation step.

- **What:** the concept or file we just created
- **Why:** the engineering reason it exists
- **Learn:** what to be able to explain in an interview

This guide is a teaching record, not a replacement for the design documents in `docs/`.

## How the project is built

Each feature goes through the same loop:

1. Pick the next item from `feature-tracker/backlog.md`.
2. Understand its acceptance criteria before writing code.
3. Implement the smallest complete version.
4. Verify it (build, test, run).
5. Update this guide and the feature tracker.
6. Commit with a message that references the tracker ID.

We always build a stable foundation before adding behavior on top of it.

## Milestone: Documentation baseline (completed)

Before any code, the project wrote ten design documents in `docs/`. This is deliberate.

- **What:** requirements, use cases, business rules, architecture, database, API, security, and testing documents.
- **Why:** scope control. A solo project fails by growing too big, not by being built too slowly. The documents decide what is in and out before code exists.
- **Learn:** you can explain the reduced MVP boundary in one sentence: *an authenticated planner creates a one-product manufacturing order that snapshots a simple BOM, reserves materials atomically, passes a fixed-stage quality gate, and completes with an all-or-nothing inventory update.*

## FND-001: Git repository hygiene (completed)

### `git init` on `main`

- **What:** turns the folder into a Git repository with a single branch called `main`.
- **Why:** version control from the first day. Every later change is reviewable and revertable.
- **Learn:** `main` always stays buildable; work happens on `feature/<tracker-id>-name` branches.

### `.gitignore`

- **What:** a list of files Git must never track: `.env` secrets, IDE folders, Maven `target/`, Node `node_modules/`, logs, OS junk.
- **Why:** build output and secrets must never enter history. Once committed, a secret is considered leaked forever.
- **Learn:** be able to name three things your `.gitignore` protects and why each is dangerous to commit.

### `.gitattributes`

- **What:** rules for how Git treats files: line endings (`LF` for shell scripts), binary files (`*.jar`), and diff behavior (`*.md`).
- **Why:** Windows uses `CRLF` line endings, macOS/Linux use `LF`. Without this rule, files show fake "whole file changed" diffs and scripts break.
- **Learn:** the difference: `.gitignore` decides *which* files are tracked; `.gitattributes` decides *how* tracked files behave.

## FND-002: Spring Boot backend bootstrap (completed)

The goal: a Spring Boot application that builds, starts, and is testable — with zero business logic. A stable shell that everything else will be built on.

### Step 1: `pom.xml`

- **What:** the Maven build file. Declares the Java version, Spring Boot version, and every dependency.
- **Why:** Maven is the single source of truth for "what this project needs to compile and run". Anyone can rebuild the project from this one file.
- **Dependencies, each justified:**
  - `spring-boot-starter-web` — REST API layer (embedded Tomcat, Spring MVC, JSON). We are building an API-first backend.
  - `spring-boot-starter-data-jpa` — Spring Data JPA and Hibernate for persistence. Declared now, used by later modules.
  - `spring-boot-starter-validation` — bean validation for DTOs at the API boundary.
  - `spring-boot-starter-security` — required soon for JWT login; declared with the foundation, not bolted on later.
  - `postgresql` — the PostgreSQL JDBC driver. The database we committed to in the design.
  - `flyway-core` + `flyway-database-postgresql` — versioned database migrations. Schema changes become reviewable source files.
  - `spring-boot-starter-test` — JUnit 5, AssertJ, Spring test support. Testing is part of the foundation, not an afterthought.
  - `spring-security-test` — lets later security tests authenticate fake users.
- **Deliberately excluded:** Redis, RabbitMQ, GraphQL, Kubernetes clients, extra utility libraries. Every dependency is a liability; the design documents say what is deferred.
- **Learn:** for every starter, one sentence: what it brings and which module will use it. Interviewers probe for "did you add this or copy it?"

### Step 2: `TextileManufacturingApplication.java`

- **What:** the main class, annotated `@SpringBootApplication`.
- **Why:** this is the entry point. The annotation is three annotations in one:
  - `@SpringBootConfiguration` — marks this class as a configuration source.
  - `@EnableAutoConfiguration` — Spring inspects the classpath and configures what it finds (JPA found + PostgreSQL driver found = datasource setup).
  - `@ComponentScan` — finds our `@RestController`, `@Service`, `@Repository` classes under this package.
- **Consequence:** the class must live at the root package (`com.textile.manufacturing`) so component scanning covers every module package we planned.
- **Learn:** explain what auto-configuration does and why the main class placement matters.

### Step 3: `application.yml`

- **What:** external configuration: server port, database connection, JPA/Hibernate behavior, Flyway settings.
- **Why:** separates *what the code does* from *where it runs*. Local Docker PostgreSQL and a future deployed database differ only by configuration.
- **Key choices:**
  - `ddl-auto: validate` — Hibernate may *check* the schema but never *change* it. Migrations own the schema.
  - Profiles: `application-local.yml` for development defaults; real secrets stay in environment variables.
- **Learn:** why `ddl-auto: update` is rejected in a real project (silent, unreviewable schema drift).

### Step 4: first smoke test

- **What:** one test that starts the Spring context and asserts the application bean exists.
- **Why:** proves all configuration is valid. Every later feature inherits this safety net; a broken configuration fails in seconds, not at runtime.
- **Learn:** what "the Spring context loads" actually verifies: beans wire, configuration parses, auto-configuration succeeds.

### Step 5: verification

- **What:** `./mvnw clean verify` passes; the app starts and logs cleanly.
- **Why:** the definition of "bootstrap complete" is mechanical, not a feeling.
- **Verified outcome (FND-002):** `./mvnw test` passed 2/2. The context loaded with the `test` profile, and the main-class bean was found by component scan.
- **Observed on purpose:**
  - Spring Security generated a default password and locked all endpoints because no security configuration exists yet. Deny-by-default is the safe direction; the IAM phase replaces this.
  - The `test` profile excludes DataSource/JPA/Flyway auto-configuration because no database exists until FND-004. This scaffolding is removed in FND-005 when Testcontainers brings real PostgreSQL into tests.
  - Mockito dynamic-agent warnings are known noise, not defects.

### Deliberately not in FND-002

No entities, no controllers, no security filters, no business modules. Those belong to their own tracker items. Building them now would mean debugging configuration and business logic at the same time.

## FND-003: React and Vite frontend bootstrap (completed)

The goal: a minimal React 19 + TypeScript + Vite 8 shell with no generated boilerplate and no business screens. Built by hand so every file is understood, not scaffolded and ignored.

### `package.json`

- **What:** the frontend equivalent of `pom.xml`: scripts and dependencies.
- **Dependencies, each justified:** `react` and `react-dom` (the UI library); dev-only `vite` (dev server and bundler), `@vitejs/plugin-react` (React fast-refresh in dev; its peer range was verified against Vite 8), `typescript`, and `@types/react*` (type definitions for a library written in plain JavaScript).
- **Scripts:** `dev` (vite dev server with hot reload), `build` (`tsc --noEmit` then `vite build`), `typecheck`, `preview` (serves the production build locally).
- **Learn:** `^` in versions means "this minor and up" — npm resolves the newest compatible patch. `devDependencies` are build-time tools; the shipped bundle contains neither Vite nor TypeScript.

### `tsconfig.json`

- **What:** TypeScript compiler options.
- **Key choices:** `strict: true` (the whole point of TypeScript), `noEmit: true` (tsc checks types; Vite does the emitting), `moduleResolution: bundler` (matches how Vite resolves imports), `verbatimModuleSyntax` (forces explicit `type` imports, aligning with modern bundling).
- **Learn:** strict mode converts silent runtime bugs (undefined access, null assumptions) into compile-time errors.

### `vite.config.ts`

- **What:** three lines configuring the React plugin.
- **Why so small:** Vite needs almost nothing by default. The API proxy for `/api` will be added in FND-005 when the frontend first calls the backend.
- **Learn:** Vite serves source directly in dev (native ES modules) and only bundles for production — that is why dev startup is instant.

### `index.html`

- **What:** the single real HTML page of the SPA.
- **Why:** a single-page application has exactly one page; React renders everything inside `#root` afterwards.
- **Learn:** the `<script type="module">` tag is what makes Vite's dev server work with unbundled source.

### `src/main.tsx`

- **What:** the entry point that mounts React into `#root`.
- **Choices:** `StrictMode` (double-invokes renders in dev to expose side effects), and an explicit fail-fast error if `#root` is missing instead of a cryptic "cannot read property of null".
- **Learn:** StrictMode is a development-time correctness tool, not a production behavior change.

### `src/App.tsx`

- **What:** the root component; intentionally a static shell with no state, no router, no styling library.
- **Learn:** the thin-client principle — screens arrive in UI-001 only after the API slices they consume exist.

### Verification (developer-run)

- `npm install`, `npm run dev` (shell visible at localhost:5173), `npm run typecheck`, `npm run build` — all passed.

## FND-004: Local PostgreSQL with Docker Compose (completed)

### `docker-compose.yml`

- **What:** one service, `postgres:18-alpine`, with a healthcheck and a named volume.
- **Choices:**
  - Pinned major version instead of `latest` — "latest" changes under your feet; the pinned image is the same for everyone, forever.
  - `${POSTGRES_PASSWORD:-textile-local-only}` interpolation — Compose reads a git-ignored `.env` (template committed as `.env.example`); defaults are local-only fake values, never real secrets.
  - Named volume at `/var/lib/postgresql` — the container is disposable, the data is not. Postgres 18's image changed the data path convention; mounting the parent directory is the documented approach.
  - `pg_isready` healthcheck — reports "ready for queries", not merely "process started".
- **Learn:** be able to explain the difference between `docker compose down` (keep data) and `docker compose down -v` (erase data), and why `POSTGRES_PASSWORD` only takes effect on first volume initialization.

### The first failure: SQL State 28P01

- **What happened:** the backend failed to start with `FATAL: password authentication failed for user "textile"`.
- **Diagnosis path:** `28P01` means wrong password; the error surfaced inside `flywayInitializer` because Flyway opens the first connection; the user existed and the network worked, so only the password could disagree.
- **Root cause:** duplicated defaults disagreed — the Compose default was `textile-local-only`, the backend fallback was `textile`.
- **Fix:** aligned the backend fallback with the documented database default. An equivalent fix would have been running with `DB_PASSWORD` set — environment always beats defaults.
- **Learn:** duplicated default values are a bug factory; when they disagree, the failure appears at runtime far from the cause. Read SQL States — they compress the whole diagnosis.

### Verification (developer-run)

- Container healthy via `docker compose ps`; database reachable through `psql`.
- `./mvnw spring-boot:run` full startup: HikariPool connected, Flyway ran, Hibernate validated an empty schema, Tomcat on 8080.
- `curl -i http://localhost:8080/api/v1/anything` returns `401` — Spring Security's deny-by-default before any configuration exists.

## FND-005: Migration, health, error, and OpenAPI foundation (completed)

### `V1__baseline.sql`

- **What:** the first Flyway migration. Contains no business tables — it establishes the mechanism and the naming convention (`V<version>__<description>.sql`).
- **Why:** migrations run once, in order, and are checksummed. Editing an applied migration fails validation; changing schema means a new migration. The schema history becomes append-only — the same philosophy the inventory ledger will use.
- **Learn:** be able to explain what `flyway_schema_history` records: version, description, checksum, timestamps, and who/what ran it.

### Actuator health

- **What:** a production-grade health probe at `/actuator/health`.
- **Choices:** exposure restricted to `health` only (Actuator can also expose shutdown, heap dumps, and env — all dangerous if left open); `show-details: never` keeps internals out of responses.
- **Learn:** health flips to `503` when any checked component (like the database) fails — this is the signal Docker healthchecks and CI pipelines watch.

### `SecurityConfig` and the 401/403 incident

- **What:** the first explicit `SecurityFilterChain`: a small public-path list, everything else `authenticated()`, CSRF disabled per the stateless bearer-token design.
- **Incident:** the web foundation test expected `401` for an anonymous call to a protected path and received `403`. Defining a custom chain removed Boot's default mechanisms, and with no mechanism configured Spring fell back to `Http403ForbiddenEntryPoint`.
- **Fix:** an explicit authentication entry point returning `401`, matching the docs/07 contract.
- **Learn:** **401 = not authenticated, 403 = authenticated but forbidden.** The entry point answers 401; the access-denied handler answers 403. Confusing them leaks information — a 403 on an unknown resource confirms it exists.

### `GlobalExceptionHandler`

- **What:** one `@RestControllerAdvice` producing RFC 9457 `ProblemDetail` errors: field-level detail for validation failures, a generic body for unexpected exceptions.
- **Why:** the API contract (docs/07) promises one error shape; stack traces and SQL go to logs, never to clients.
- **Learn:** the catch-all `Exception` handler is a safety net, not a design — specific domain exceptions will get specific handlers as modules grow.

### springdoc OpenAPI

- **What:** Swagger UI at `/swagger-ui.html`, generated from the code (springdoc 2.8.17 — the line supporting Boot 3.5; 3.x targets Boot 4).
- **Why:** documentation that cannot drift from the implementation, because it is generated from it.
- **Learn:** OpenAPI is a contract review artifact here, not decoration — every module endpoint added later appears here automatically.

### Verification (developer-run)

- `./mvnw test` — 4/4 (context load, main-class bean, health public, protected path 401).
- Runtime: health `{"status":"UP"}`, anonymous API call `401`, `flyway_schema_history` present in psql, Swagger UI reachable.

## Scope Revision: Reduced MVP (approved)

The original design described ten broad business areas. That was intentionally reduced before deeper implementation.

- **Keep:** Identity, Catalog, Inventory, and Manufacturing.
- **Keep technical depth:** JWT, DTOs, validation, JPA, Flyway, PostgreSQL, Docker, REST, transactions, row locking, rollback tests, and a thin React client.
- **Simplify:** customer demand is one manufacturing order with a customer name/reference; BOM is one active simple BOM; stages are fixed fields; quality is one pass/fail inspection.
- **Defer:** customer master, multi-line orders, BOM revisions, separate stage tables, detailed defects/rework, warehouses, procurement, scheduling, and external infrastructure.
- **Why:** fewer business lifecycles leave more time to prove consistency, security, concurrency, and transaction behaviour.
- **Future path:** deferred items remain documented as `EXT-*` extensions in the project overview and feature tracker; they were not forgotten or silently removed.

## IAM-001: Internal users and fixed roles (in progress)

### Step 1: `V2__identity_users_and_roles.sql`

- **What:** the first business tables: `role` (seeded), `app_user`, `user_role`.
- **Decisions:**
  - Roles are seeded by the migration with fixed literal UUIDs — reference data is owned by migrations, not runtime code, so the authorization model cannot drift between environments.
  - `email` (as typed) plus `email_normalized` (lowercased, `UNIQUE`) — case-insensitive identity with a plain unique index. Rejected alternative: a functional index on `lower(email)`.
  - `user_role` has a composite primary key `(user_id, role_id)` — the pair *is* the identity, so duplicate assignments are impossible by construction.
  - `status` has a `CHECK` constraint; foreign keys are restrictive (no `ON DELETE`) because users appear in future audit records and are deactivated, never deleted.
- **Learn:** defense in depth — the service validates for good error messages; the database constraint is the last line that holds even when a bug bypasses the service.
- **Verified:** developer saw the three tables and five seeded roles.

### Step 2: entities and repositories

- **What:** `AppUser`, `Role`, `UserStatus` in `identity/domain`; two Spring Data repositories in `identity/repository`.
- **Decisions:**
  - The entity has no setters: `AppUser.register(...)` is the only creation path (trims, normalizes, requires a role, starts `ACTIVE`), and `deactivate()` is the only status change. Domain model, not a data bag.
  - `@Enumerated(EnumType.STRING)` — the default `ORDINAL` stores array positions; reordering the enum would silently corrupt rows.
  - `@ManyToMany(fetch = EAGER)` for roles — normally an anti-pattern, correct here: a fixed five-row set needed on every authenticated request, and lazy loading fails in security filters where no transaction is open.
  - `@Version` (optimistic locking), `@CreationTimestamp`/`@UpdateTimestamp`, protected no-arg constructor (Hibernate requirement), `getRoles()` returns an unmodifiable copy.
  - Repositories derive queries from method names (`findByEmailNormalized`, `findByCodeIn`) — zero SQL written.
- **Verified:** `spring-boot:run` started cleanly — `ddl-auto: validate` compared every mapping against the `V2` schema and approved it. This is the payoff of `validate` over `update`.

### Step 3: service layer

- **What:** `UserService` (create/list/get/deactivate), a `BCryptPasswordEncoder` bean, and three typed exceptions (`UserNotFoundException`, `DuplicateEmailException`, `UnknownRoleException`).
- **Decisions:**
  - Hashing happens in the service (application policy); the entity only requires that *a* hash was provided.
  - `@Transactional` on commands, `readOnly = true` on queries — one boundary for everything a command must atomically change.
  - `deactivateUser` has no explicit `save()` — managed entities flush changes at commit (dirty checking).
  - Honest gap: duplicate-email check is check-then-insert; two concurrent creates race, and the `UNIQUE` constraint decides the winner. The loser surfaces as a constraint violation that the API layer must map to `409` (Step 4).
- **Incident 1:** `Pageable` was imported from `java.util` — it lives in `org.springframework.data.domain`. The first compiler error told the whole story: `location: package java.util`. Read the first error; later ones are usually noise.
- **Incident 2 — the scaffolding expired:** tests failed with `No qualifying bean of type 'AppUserRepository'`. Cause: the FND-002 test-profile exclusions (DataSource/JPA/Flyway auto-configuration) meant no repository beans could exist; the moment `UserService` — a real bean — entered the context, it had nothing to inject. Exclusion-based scaffolding has a known expiry date: the first real bean that depends on what was excluded. The fix removed the scaffolding entirely: a `IntegrationTestBase` with a singleton Testcontainers PostgreSQL now backs all tests, migrations run in the test context, and the condition-evaluation report (the huge "Did not match" dump) was the failure analyzer showing exactly which infrastructure was switched off.
- **Verified:** `./mvnw test` passes 4/4 against a real PostgreSQL container; test schema is created by the actual Flyway migrations.

### Step 4: REST API

- **What:** `UserController` (list/create/get/deactivate), `UserResponse` and `CreateUserRequest` DTOs, `PageResponse<T>`, `@PreAuthorize("hasRole('ADMIN')")`, and exception mappings (`404`, `409`, `422`, plus `DataIntegrityViolationException -> 409` closing the check-then-insert race).
- **Decisions:**
  - `UserResponse` cannot leak `passwordHash` — the DTO simply has no such field.
  - `PageResponse` replaces Spring's `PageImpl` JSON (unstable across framework versions) with the documented envelope.
  - Deactivation is a command subresource (`POST /{id}/deactivation`), never a status PATCH — lifecycle rules cannot be bypassed.
  - `@EnableMethodSecurity` turned on endpoint-level authorization; service-level checks remain the second layer.

### Step 5: API tests and the security-exception incident

- **What:** `UserApiTests` — 7 MockMvc tests covering 201/200/409/404/400/403/401, including an explicit assertion that `passwordHash` never appears in responses.
- **Teaching points:**
  - `@WithMockUser(roles = "ADMIN")` injects a fake principal (authority `ROLE_ADMIN`) — it tests authorization wiring before real JWT exists.
  - Each test uses a unique email because tests share one database and JUnit does not guarantee method order.
- **Incident 3 — the catch-all advice swallowed security exceptions:** the PLANNER-denied test expected `403` and received `500`. `AccessDeniedException` thrown by `@PreAuthorize` inside the controller dispatch reached the catch-all `@ExceptionHandler(Exception.class)`, which converted an authorization denial into a server error. Fix: the advice rethrows `AccessDeniedException`/`AuthenticationException` so Spring Security's `ExceptionTranslationFilter` translates them (`403`/`401`). Security exceptions belong to the security layer, not the MVC error layer.
- **Verified:** `./mvnw test` passes 11/11.

## IAM-002: JWT login and server-side authorization (in progress)

### Step 1: token infrastructure

- **What:** `jjwt` 0.13.0 dependency (api/impl/jackson split), `JwtProperties` (`app.security.jwt.*` with `JWT_SECRET` env override, 15-minute expiry), and `JwtTokenService` (issue + parse).
- **Decisions:**
  - jjwt instead of Spring's `oauth2-resource-server` — explicit token handling over deep abstraction for a security-showcase project; resource-server reconsidered for real multi-service deployments.
  - HMAC with an environment-provided secret; `Keys.hmacShaKeyFor` enforces the 256-bit minimum at startup.
  - Claims: `sub` (user UUID), `roles` (client display only), `iss`, `iat`, `exp`.
- **Verified:** `./mvnw test` 11/11 with the new beans in the context.

### Step 2: login flow and bootstrap admin

- **What:** `AuthenticationService.authenticate` (normalize → find → BCrypt match → active check → user), `AuthController` (`POST /api/v1/auth/login`, `GET /api/v1/auth/me`), `LoginRequest`/`LoginResponse` DTOs, `InvalidCredentialsException` mapped to a Problem Detail `401`, `V3__seed_admin_user.sql` (bootstrap admin with BCrypt hash, fixed UUID), and `/api/v1/auth/login` added to the public-path list.
- **Decisions:**
  - Every failure (unknown email, wrong password, inactive account) throws the same `InvalidCredentialsException` — no user enumeration.
  - The `401` for bad credentials is produced by the advice (a business failure on a public endpoint); the rethrow pattern stays reserved for framework security exceptions.
  - The admin seed is migration-owned like the role seed; the committed password is a documented local-only value that must be rotated or removed before real deployment.
  - `issueToken` now returns an `IssuedToken` record (token + `expiresAt`) so the response can tell the client when re-login is needed.
- **Observed detail:** the issued token's header shows `alg: HS384`, not HS256 — jjwt selects the *strongest HMAC variant the key material supports*; the local default secret is long enough for 384 bits. Compatible with the documented HMAC design; a production secret choice may pin the algorithm explicitly.
- **Verified:** developer ran `curl` login — `200` with `accessToken`, `expiresAt`, and the user DTO (`passwordHash` absent); wrong password returns `401`.
- **Not yet possible:** `GET /auth/me` and protected endpoints still return `401` even with a valid token, because no filter reads the `Authorization` header yet. That is Step 3.

### Step 3: the JWT authentication filter

- **What:** `JwtAuthenticationFilter` (OncePerRequestFilter), `AuthenticatedUserProvider` port + `AuthenticatedUser` record, `DatabaseAuthenticatedUserProvider` implementation, and `SecurityConfig` wiring with `STATELESS` sessions and `addFilterBefore`.
- **Decisions:**
  - Dependency inversion: `common/security` defines the provider interface; `identity` implements it with the repository. The filter never touches entities — `common` stays technical-only.
  - Three filter paths: no/invalid header passes through anonymously (protected routes get `401` from the entry point); invalid/expired tokens clear the context and continue (`401`, never `500` — invalid client input is not a server error); valid tokens load the user and populate the context.
  - Roles are reloaded from the database per request; the token's `roles` claim is display-only. `DatabaseAuthenticatedUserProvider` filters `INACTIVE`, so a deactivated user's still-valid token immediately stops working — revocation without waiting for expiry.
  - `AuthenticatedUser implements UserDetails` so `getName()` returns the user UUID and authorities are `ROLE_*`.
  - The filter is constructed inside `SecurityConfig`, not registered as a bean — a filter bean would also be auto-registered for all servlet requests and run twice.
  - `SessionCreationPolicy.STATELESS` — no `JSESSIONID`; every request authenticates independently.
- **Compile lesson:** `List<SimpleGrantedAuthority>` cannot be assigned to `List<GrantedAuthority>` (generics invariance) — the override declares the subtype list, which is covariantly compatible with the interface's `Collection<? extends GrantedAuthority>`.

### Step 4: real-token tests

- **What:** `AuthApiTests` (6 tests through real HTTP login → token → Bearer headers, no `@WithMockUser`) and `JwtTokenServiceTests` (2 unit tests).
- **Key tests:** identical `401` for unknown email and wrong password (anti-enumeration, asserted); tampered token rejected; the **revocation test** — planner's valid token stops working immediately after admin deactivation; expired-token test uses a 0-minute configuration instead of sleeping in the test.
- **Incident — expiry precision:** the roundtrip test compared expiry microseconds-for-microseconds and failed: RFC 7519 stores time claims as NumericDate *seconds*. The token was correct; the assertion was wrong. Assert at the precision the standard actually guarantees (`truncatedTo(ChronoUnit.SECONDS)`).
- **Verified:** `./mvnw test` passes 19/19; developer confirmed end-to-end with curl: login → `200` with token, `/me` and `/users` with Bearer → `200`, corrupted token → `401`.
- **Known noise:** the "generated security password" warning appears only in the test context (Spring's unused in-memory default user); optional cleanup is to exclude `UserDetailsServiceAutoConfiguration` in tests or provide a `UserDetailsService` bean.

## CAT-001: Products and materials (completed)

### Step 1: `V4__catalog_products_and_materials.sql`

- **What:** `product` and `material` tables with dual code columns, status/archive fields, and unit CHECK constraints.
- **Why:** archive instead of delete (referenced records stay readable), normalized code for case-insensitive uniqueness, database as backstop for the documented unit set.
- **Verified:** developer saw both tables after startup.

### Step 2: entities and repositories

- **What:** `Product`/`Material` entities under `catalog/product` and `catalog/material`, two status enums, two unit enums, minimal repositories.
- **Why:** separate output-unit and base-unit enums instead of one shared `Unit` — product outputs and material inputs are different concepts that merely overlap today.
- **Verified:** `./mvnw test` 19/19 (mappings validated against `V4`).

### Step 3: services and grouped error mapping

- **What:** `ProductService`/`MaterialService`, typed exceptions, grouped handlers (`404`, `409`).
- **Why:** two small services keep each aggregate's ownership clear; the unique index remains the race referee behind the friendly duplicate check.

### Step 4: REST API and the enum-binding lesson

- **What:** `ProductController`/`MaterialController` with DTOs, role-gated writes, command-subresource archival, and the `OpenApiConfig` bearer scheme.
- **Discovery:** Swagger UI had no Authorize button until the OpenAPI document declared a bearer security scheme — springdoc scans controllers, not security config.
- **Incident:** the first DTO version held the unit as a `String` and converted with `valueOf` in the controller — an invalid unit (`INCHES`) threw `IllegalArgumentException` deep inside the app and returned `500`. Fix: bind the enum directly in the DTO; unknown names fail at the JSON boundary through the `HttpMessageNotReadableException` handler with a clean `400`. Type-safe binding beats manual conversion.
- **Second lesson:** the GET-read test initially forgot the `Authorization` header and received `401` — correct API, wrong test. Every endpoint requires authentication, including reads.

### Step 5: tests

- **What:** `CatalogDomainTests` (entity state machine without HTTP/DB) and `CatalogApiTests` (real-token CRUD, duplicates, immutable code, one-way archive, role matrix, invalid enum).
- **Also fixed:** updating an archived product threw `IllegalStateException` which would have surfaced as `500`; it is a domain conflict, now mapped to `409 Invalid state`.
- **Verified:** `./mvnw test` passes 30/30; developer created the first product through Swagger UI with a real login.

## Rules for this guide

- Every completed step gets an entry: what, why, learn.
- Entries are written after verification, not before.
- If a later decision reverses an earlier one, add a note under the old entry explaining why. Do not silently rewrite history.

## Rules for how we work

- The AI creates files, explains decisions, and prepares commands.
- The developer runs every build, test, and verification command personally, and reads the output.
- Verification is only trusted once the developer has seen it pass with their own command run.
