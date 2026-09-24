# Completed Features and Milestones

No domain workflow feature has been completed yet. The following planning and foundation milestones are complete.

## DOC-001: Documentation Baseline

- **Type:** Project milestone
- **Evidence:** `README.md` and `docs/01` through `docs/10`

## STR-001: Initial Repository Skeleton

- **Type:** Project milestone
- **Evidence:** `backend/` and `frontend/` structure with no executable implementation

## FND-001: Git Repository and Project Hygiene

- **Type:** Foundation feature
- **Evidence:** Git repository initialized on `main`; `.gitignore` and `.gitattributes` added; branch, commit, and command conventions documented in `README.md`; no secrets, environment files, or generated output tracked.

## FND-002: Spring Boot Backend Bootstrap

- **Type:** Foundation feature
- **Evidence:** `backend/pom.xml` (Spring Boot 3.5.16, Java 21 target, justified dependencies only); main class at the root package for full component-scan coverage; `application.yml` with env-var configuration, `ddl-auto: validate`, `open-in-view: false`, UTC; Maven Wrapper added; smoke test verifies context load and main-class registration (`./mvnw test`, 2/2 passed). Full startup against PostgreSQL is verified in FND-004.

## FND-003: React and Vite Frontend Bootstrap

- **Type:** Foundation feature
- **Evidence:** Hand-crafted minimal shell (no generated boilerplate): `package.json` (React 19.2.8, Vite 8.2.2, plugin-react 6.1.1 with verified peer pairing, TypeScript 5.9, strict mode), `tsconfig.json`, `vite.config.ts`, `index.html`, `src/main.tsx` with fail-fast root check, `src/App.tsx`. Developer verified all four commands: `npm install`, `npm run dev`, `npm run typecheck`, `npm run build`.

## FND-004: Local PostgreSQL with Docker Compose

- **Type:** Foundation feature
- **Evidence:** `docker-compose.yml` (postgres:18-alpine, env-interpolated credentials, named volume at the PG18 data path, `pg_isready` healthcheck); `.env.example` template with `.env` git-ignored; Compose commands documented in README. Developer verified: container healthy, database reachable, and `./mvnw spring-boot:run` full startup against PostgreSQL. First failure (SQL State 28P01 password mismatch between backend fallback and database initialization) diagnosed and fixed by aligning defaults; lesson recorded in the learning guide.

## FND-005: Migration, Health, Error, and OpenAPI Foundation

- **Type:** Foundation feature
- **Evidence:** `V1__baseline.sql` applied on a clean database (`flyway_schema_history` visible via psql); actuator health restricted to the health endpoint (`{"status":"UP"}` public, DB down would report 503); `SecurityConfig` with explicit public paths, deny-all otherwise, and a custom authentication entry point returning 401 (the default returned 403 for anonymous callers — caught by the web foundation test and fixed against the docs/07 contract); `GlobalExceptionHandler` returning RFC 9457 Problem Details with safe 500 responses; springdoc 2.8.17 serving Swagger UI. Developer verified: 4/4 tests, health curl, 401 curl, migration table, Swagger UI.

## SCOPE-002: Reduced MVP Scope

- **Type:** Planning milestone
- **Evidence:** Documentation and backlog revised from ten broad business areas to four logical areas: Identity, Catalog, Inventory, and Manufacturing. Customer master, multi-line orders, BOM revisions, separate stage tables, detailed quality, and advanced ERP features are preserved as deferred extensions.

## IAM-001: Internal Users and Fixed Roles

- **Type:** Domain feature (Identity)
- **Evidence:** `V2__identity_users_and_roles.sql` (fixed seeded roles, dual email columns, composite user_role key, status check); `AppUser` domain model with register/deactivate invariants; `UserService` with BCrypt, transaction boundaries, and typed exceptions; `UserController` with DTO validation, 201/Location, admin-only `@PreAuthorize`, and `PageResponse` pagination; `GlobalExceptionHandler` mappings for 404/409/422 plus the security-exception rethrow fix (catch-all advice previously converted authorization denials into 500s — caught by `UserApiTests`). Developer verified: `./mvnw test` passes 11/11 against the Testcontainers PostgreSQL base.

## IAM-002: JWT Login and Server-Side Authorization

- **Type:** Domain feature (Identity)
- **Evidence:** jjwt 0.13.0 with `JwtProperties` (env-provided secret, issuer, 15-minute expiry); `JwtTokenService` (issue/parse with signature, issuer, and expiry validation); `AuthenticationService` with generic `InvalidCredentialsException` (no user enumeration); `AuthController` (`POST /api/v1/auth/login` public, `GET /api/v1/auth/me` authenticated); `V3__seed_admin_user.sql` bootstrap admin; `JwtAuthenticationFilter` wired before the auth filter chain with `STATELESS` sessions; `AuthenticatedUserProvider` port implemented by `DatabaseAuthenticatedUserProvider` so tokens reload authoritative active status and roles from the database on every request (revocation without waiting for token expiry). Tests: `AuthApiTests` (login contract, identical 401 for unknown email/wrong password, real-token protected access, tampered token, deactivation revocation) and `JwtTokenServiceTests` (roundtrip, second-granularity expiry per RFC 7519, zero-expiry rejection). Developer verified: `./mvnw test` passes 19/19; manual curl login, `/me`, and `/users` with Bearer token returned 200.

## Identity Phase Complete

`IAM-001` and `IAM-002` are complete: administrator user management, BCrypt credentials, JWT login, token validation filter, and role-based authorization enforced by endpoint and service layers with automated evidence.

## CAT-001: Products and Materials

- **Type:** Domain feature (Catalog)
- **Evidence:** `V4__catalog_products_and_materials.sql` (dual code columns, status checks, unit CHECK constraints, status indexes); `Product`/`Material` domain models with register/update/archive invariants and separate unit enums; `ProductService`/`MaterialService` with duplicate checks and status filtering; full REST API (`/api/v1/products`, `/api/v1/materials`) with DTO validation, role-gated writes (ADMIN/PLANNER), command-subresource archival, and OpenAPI bearer scheme (Authorize button); `IllegalStateException -> 409` and malformed-body/type-mismatch -> `400` error mappings. Tests: `CatalogDomainTests` (entity state machine) and `CatalogApiTests` (real-token CRUD, duplicate 409, immutable code, archive one-way, invalid enum 400, operator-role 403, material duplication). Developer verified: `./mvnw test` passes 30/30 and manual Swagger flow created the first product end-to-end.

## Foundation Phase Complete

`FND-001` through `FND-005` are complete: version-controlled repository, buildable backend shell, buildable frontend shell, containerized PostgreSQL, and the operational foundation (migrations, health, error contract, API documentation). The next phase is identity and access.

When an application feature is complete, add it only after its acceptance criteria, tests, security review, and documentation are finished. Link to the relevant implementation or test paths as evidence.
