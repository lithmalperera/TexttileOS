# Testing Strategy

**Project:** Textile Manufacturing Management System  
**Document status:** Proposed  
**Implementation status:** Documentation only

The testing strategy prioritizes business correctness and persistence behaviour over a large number of shallow tests. The most important evidence will be that inventory cannot be oversold, workflow transitions cannot be bypassed, and production completion is atomic.

## 1. Testing Goals

- Detect invalid business state transitions before they reach the database.
- Verify relational constraints and transaction behaviour against PostgreSQL.
- Prove that competing inventory commands cannot create an impossible balance.
- Verify authentication and authorization independently of frontend visibility.
- Trace important requirements and business rules to executable tests.
- Give the frontend a stable API contract and a small number of workflow smoke tests.
- Keep tests deterministic, isolated, readable, and useful during refactoring.

## 2. Proposed Test Stack

| Area | Proposed tool or approach | Reason |
| --- | --- | --- |
| Backend unit tests | JUnit 5, AssertJ, and focused mocks where needed | Standard Java/Spring support and readable assertions |
| Spring web tests | Spring Boot test support with MockMvc or WebTestClient | Verifies routing, DTO validation, security, and error mapping |
| Persistence integration | Spring Boot test support with Testcontainers PostgreSQL | Exercises the real database dialect, constraints, and transaction behaviour |
| Migration verification | Run the same versioned migrations used by the application in a clean PostgreSQL test database | Prevents drift between test assumptions and the actual schema |
| Frontend unit/component tests | Vitest and React Testing Library | Covers client state, form behaviour, and error/loading states without requiring a browser for every test |
| Browser smoke tests | Playwright as a small later addition | Proves that a reviewer can complete the main flow through the actual client |
| Static checks | Java/TypeScript compilation, formatting, and selected static analysis | Catches defects before runtime tests and keeps the codebase consistent |

Testcontainers, Vitest, React Testing Library, and Playwright are supporting test tools, not changes to the requested application architecture. They should be introduced only when the corresponding implementation phase begins.

## 3. Test Pyramid

```text
                  +---------------------+
                  | Browser smoke tests |
                  +---------------------+
                +-------------------------+
                | API/workflow tests      |
                +-------------------------+
              +-----------------------------+
              | Repository and transaction |
              | integration tests          |
              +-----------------------------+
            +---------------------------------+
            | Domain and service unit tests   |
            +---------------------------------+
```

The base should be fast unit tests for calculations and state transitions. The smaller upper layers provide confidence that the modules, database, security filters, and client work together.

## 4. Test Levels and Ownership

### 4.1 Domain and service unit tests

Use unit tests for rules that can be evaluated without HTTP or a real database:

- positive quantity and unit validation
- BOM requirement calculation
- BOM revision activation rules
- order and production state transitions
- stage ordering
- quality defect quantity validation
- reservation command decisions using mocked inventory responses where persistence is not the subject
- role-to-operation policy decisions where appropriate

Unit tests should assert business outcomes and meaningful error codes, not private method calls or implementation details.

### 4.2 Repository and persistence integration tests

Use PostgreSQL-backed tests for:

- foreign keys and uniqueness
- partial unique indexes such as one active BOM per product
- normalized code uniqueness
- numeric precision and constraints
- migration compatibility
- repository queries and pagination
- stock balance locking and updates
- append-only inventory movement persistence
- rollback of multi-record operations

H2-only tests are not sufficient for inventory locking, PostgreSQL checks, or dialect-specific query behaviour.

### 4.3 API and security tests

Use Spring web tests for:

- request DTO validation and error shape
- authentication success and failure
- missing, malformed, and expired JWT handling
- role-based authorization
- response DTO shape and status codes
- no direct exposure of persistence fields
- not-found and conflict behaviour
- pagination and filter parameter validation

These tests should call the HTTP layer but can use isolated service or repository setup depending on the behaviour being verified.

### 4.4 Workflow integration tests

Use a real Spring application context and PostgreSQL for the most important cross-module flows:

1. Create master data and activate a BOM.
2. Record material stock.
3. Confirm a customer order.
4. Create production from one order line.
5. Reserve material.
6. Complete production stages.
7. Record a passing inspection.
8. Complete production.
9. Assert material consumption, finished-product receipt, order state, and movement history.

The same workflow suite should cover failure branches such as missing BOM, insufficient stock, failed quality, invalid stage transitions, and completion rollback.

### 4.5 Concurrency tests

At least one integration test must execute two reservation commands concurrently against limited stock:

1. Arrange a stock balance with enough quantity for exactly one production order.
2. Create two independent production orders requiring the same material.
3. Start two transactions at the same time using a coordination barrier.
4. Allow both commands to attempt reservation.
5. Assert that exactly one succeeds and the other receives a documented conflict or insufficiency result.
6. Assert that on-hand quantity is unchanged, reserved quantity equals the winning reservation, and no partial movement exists for the failed command.

Additional concurrency cases should cover:

- two reservations with multiple material lines
- completion competing with cancellation
- duplicate stage completion requests
- stale edits using aggregate version values if optimistic checks are implemented

The test must use PostgreSQL and real transactions. A single-threaded mock cannot prove the required behaviour.

### 4.6 Frontend tests

Component and client tests should cover:

- login success and authentication failure
- role-based visibility as a usability feature, while remembering backend authorization is authoritative
- form validation and server-side error display
- loading, empty, conflict, and permission-denied states
- displaying decimal quantities without rounding them incorrectly
- advancing the production workflow through API responses
- quality failure preventing the completion action in the UI

The frontend should not test the backend's material formula as its own calculation. It should test that the returned result is rendered and acted upon correctly.

### 4.7 Browser smoke test

After the API and frontend are stable, add one browser-level smoke scenario using seeded or fixture data:

```text
Login -> inspect product/BOM -> inspect stock -> confirm order
-> create production -> reserve material -> advance stages
-> pass quality -> complete production -> view movements
```

This test is a release confidence check, not a replacement for unit and integration coverage. It should avoid asserting every visual detail.

## 5. Requirement Traceability

The following mapping is the minimum traceability plan:

| Requirement area | Primary test evidence |
| --- | --- |
| `FR-IAM-*` and `NFR-SEC-*` | Authentication API tests, authorization matrix tests, security service tests |
| `FR-PRD-*`, `FR-MAT-*`, `FR-BOM-*` | Service unit tests, repository constraints, BOM activation integration tests |
| `FR-INV-*` and `NFR-CON-*` | Inventory service tests, PostgreSQL transaction tests, concurrency tests, movement assertions |
| `FR-CUS-*`, `FR-ORD-*` | Order service and API tests, status transition tests |
| `FR-PROD-*`, `FR-STG-*` | Production and stage service tests, workflow integration tests |
| `FR-QLT-*` | Quality service/API tests and completion-gate integration tests |
| `FR-API-*` | Controller tests, OpenAPI review, error-contract tests |
| `FR-UI-*` and `NFR-UX-*` | React component tests and one browser smoke scenario |
| `NFR-DATA-*` | PostgreSQL schema, migration, constraint, and repository tests |
| `NFR-DOC-*` | Review checklist comparing docs, OpenAPI, and implemented behaviour |

Each detailed test should reference a requirement or business-rule identifier in its name, test display name, or test documentation when that improves traceability.

## 6. Critical Test Catalogue

### Identity and authorization

- `SEC-01`: unauthenticated request is rejected.
- `SEC-02`: valid token with permitted role succeeds.
- `SEC-03`: valid token with insufficient role receives `403`.
- `SEC-04`: expired or malformed token receives `401`.
- `SEC-05`: deactivated user cannot authenticate or use protected operations according to the active-user policy.
- `SEC-06`: credentials, bearer tokens, and password hashes do not appear in errors or logs.

### Master data and BOM

- `BOM-01`: duplicate product or material code is rejected.
- `BOM-02`: inactive material cannot be added to a draft that is activated.
- `BOM-03`: only one BOM revision is active for a product.
- `BOM-04`: active BOM becomes immutable.
- `BOM-05`: requirements equal production quantity multiplied by quantity per product unit.
- `BOM-06`: changing the active BOM does not alter a production snapshot.

### Inventory

- `INV-01`: receipt updates balance and creates a movement.
- `INV-02`: invalid adjustment cannot create negative on-hand stock.
- `INV-03`: available equals on-hand minus reserved.
- `INV-04`: insufficient multi-line reservation makes no balance change.
- `INV-05`: competing reservations cannot oversell.
- `INV-06`: release reduces reserved quantity but not on-hand quantity.
- `INV-07`: consumption reduces on-hand and reserved quantities together.
- `INV-08`: inventory movement history is append-only.

### Orders, production, and stages

- `FLOW-01`: unconfirmed order line cannot create production.
- `FLOW-02`: production without an active BOM is rejected atomically.
- `FLOW-03`: one order line cannot create two production orders.
- `FLOW-04`: stages cannot start out of order.
- `FLOW-05`: duplicate stage completion is rejected.
- `FLOW-06`: final stage moves production to quality pending.

### Quality and completion

- `QUAL-01`: inspection cannot be submitted before all stages complete.
- `QUAL-02`: defect quantity cannot exceed production quantity.
- `QUAL-03`: failed quality blocks completion and creates no completion movements.
- `QUAL-04`: passing quality permits completion only with an active reservation.
- `QUAL-05`: completion creates material and finished-product movements exactly once.
- `QUAL-06`: injected persistence failure rolls back completion state and inventory changes.

## 7. Test Data and Fixture Strategy

- Use migration-provided reference data only for fixed roles, units, and stage definitions.
- Build business fixtures in test code or dedicated test builders so each test states its important data.
- Do not rely on execution order or a shared mutable database between tests.
- Reset or recreate the PostgreSQL test database between integration test contexts as needed.
- Use realistic quantities that exercise decimal precision, insufficient stock, and boundary values.
- Keep demo data separate from automated test fixtures.
- Use fake users and customers; never use personal or production data.

## 8. Transaction and Rollback Verification

For every multi-record command, test both commit and rollback:

| Command | Commit assertion | Rollback assertion |
| --- | --- | --- |
| BOM activation | New revision active and previous revision retired | No revision state changes if activation fails |
| Inventory receipt | Balance and movement exist | Neither exists after failure |
| Reservation | All balances, reservation lines, and movements exist | No line or balance is partially reserved |
| Release | Reserved quantity and release movements are correct | Original reservation remains if release fails |
| Completion | Material consumption, finished receipt, and statuses agree | No partial stock or status update remains |
| Cancellation | Reservation is released and cancellation recorded | Original production state remains if release fails |

Where a failure is difficult to induce naturally, use a test seam or controlled repository failure rather than production-only flags.

## 9. Test Environment and CI Plan

### Local development

- Unit tests should run without Docker where possible.
- PostgreSQL-backed integration tests should start a disposable database automatically or use the documented local database profile.
- Developers should be able to run the full backend test suite with one documented command.
- Frontend tests should run independently and as part of the full project verification command when the frontend exists.

### Continuous integration

After the core build is stable, a lightweight GitHub Actions workflow may run:

1. backend compilation and unit tests
2. backend PostgreSQL integration tests
3. frontend type checking and tests
4. formatting or static checks
5. optional build artefact verification

CI should execute existing quality checks. It should not become a separate deployment platform or require Kubernetes.

## 10. Coverage and Quality Gates

The project should not claim that a single line-coverage percentage proves correctness. Instead, use these gates:

- every MUST business rule has at least one automated test
- every production and inventory state transition has success and rejection coverage
- every multi-record transaction has commit and rollback coverage
- at least one real PostgreSQL concurrency test passes
- every protected command has authorization coverage
- API error shapes and important status codes are tested
- the core end-to-end flow passes from a clean database
- the frontend has one successful and one failed path for the core workflow

Coverage reports can be used to find untested code, but they should not drive unnecessary tests for generated configuration or trivial accessors.

## 11. Definition of Done for Tests

A feature is test-complete when:

- its service rules are covered by fast unit tests
- its persistence constraints or queries are covered by PostgreSQL integration tests where relevant
- its HTTP contract is covered by controller/API tests
- its authorization behaviour is tested
- its failure and rollback paths are covered
- its requirement and business-rule references are updated
- tests are deterministic and can run from a clean environment

## 12. Testing Decisions

| Decision | Why it is appropriate | Alternative rejected | Interview and reengineering benefit |
| --- | --- | --- | --- |
| Use a layered test pyramid | Fast unit tests provide feedback while fewer integration and browser tests prove real boundaries | Test only through the browser or only through mocks | Demonstrates test-level judgement and keeps feedback practical |
| Use real PostgreSQL for persistence-sensitive tests | Inventory locking and constraints depend on database behaviour | Use H2 for all integration tests | Avoids false confidence from a different SQL dialect and isolation model |
| Test the complete workflow before broad UI coverage | The project value is the business flow across modules | Build many isolated screen tests first | Detects integration errors early and creates a strong portfolio demonstration |
| Include explicit concurrency tests | Reservation correctness is a stated engineering goal, not an incidental implementation detail | Assume sequential unit tests prove concurrency | Demonstrates understanding of race conditions and transaction isolation |
| Test business commands rather than generic setters | State transitions and inventory operations have meaningful invariants | Test only getters, setters, and controller status codes | Keeps tests aligned with domain risk and protects refactoring |
| Avoid a universal 100 percent coverage target | Generated/configuration code is less risky than inventory and state transitions | Optimize every line equally | Shows risk-based quality judgement rather than metric chasing |
| Add browser smoke testing after API stability | A single user-path check validates the real client without making browser tests the whole suite | Build a large brittle end-to-end suite from the beginning | Balances reviewer confidence with maintainable feedback time |
