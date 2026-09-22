# Development Plan

**Project:** Textile Manufacturing Management System  
**Document status:** High-level plan  
**Current phase:** Planning and documentation only

This plan sequences the work so that one developer can reach a complete, demonstrable MVP without building infrastructure or UI that the core workflow does not need. It is a delivery plan, not an instruction to begin implementation during the documentation phase.

## 1. Delivery Principles

- Keep one deployable Spring Boot modular monolith and one PostgreSQL database.
- Build in vertical slices. Each slice should include its API boundary, service rules, persistence, tests, and documentation before the next slice expands the scope.
- Establish the main transaction and state-transition rules before polishing the frontend.
- Prefer an end-to-end workflow with meaningful failure cases over isolated CRUD modules.
- Use versioned database migrations from the first schema change. Flyway is the recommended small supporting tool; it complements JPA rather than replacing it.
- Test database-sensitive behaviour with PostgreSQL, preferably through a disposable integration-test database such as Testcontainers once the test setup is established. H2-only tests should not be treated as sufficient for locking and constraint behaviour.
- Keep optional infrastructure out until the core flow is stable and a measured requirement justifies it.
- Treat the documentation as part of the implementation: update the relevant design or API document when a business rule changes.

## 2. Target Repository Shape

The following is the intended structure after implementation begins. It is shown here for planning only; these application directories should not be created as part of this documentation step.

```text
/
  backend/             Spring Boot modular monolith
  frontend/            React + TypeScript + Vite client
  docs/                Project, domain, API, security, and testing documentation
  docker-compose.yml   Local development services
  README.md
```

The backend should be organized by module rather than by one application-wide layer. The frontend should be organized around a small number of workflow screens and API clients rather than a large design system.

## 3. High-Level Sequence

| Phase | Focus | Main exit criteria |
| --- | --- | --- |
| 0 | Planning and scope control | Scope, module boundaries, business flow, and delivery sequence are reviewed |
| 1 | Project foundation | The local development foundation can start, connect to PostgreSQL, and expose a basic documented application shell |
| 2 | Identity and access | A user can authenticate and role-protected operations are enforced and tested |
| 3 | Product, material, and BOM master data | Valid active product definitions and versioned BOMs can be maintained |
| 4 | Inventory and reservations | Stock changes are auditable and concurrent reservations remain consistent |
| 5 | Customers and customer orders | A confirmed order line can become eligible for production |
| 6 | Production and stages | A production order can snapshot its BOM, reserve material, and progress through controlled stages |
| 7 | Quality and end-to-end completion | A passing inspection completes production and updates inventory transactionally |
| 8 | API contract and thin frontend | A reviewer can exercise the main workflow through documented REST endpoints and a focused UI |
| 9 | Hardening and portfolio release | Tests, security, documentation, local setup, and demonstration data are ready for review |

The phases are intentionally ordered by dependency. For example, the frontend is not the first milestone because it would otherwise be built against unstable business rules and API responses.

## 4. Phase Details

### Phase 0: Planning and Scope Control

**Status:** Current phase

Deliverables:

- Root README and initial project overview.
- MVP functional and non-functional requirements.
- Primary use cases and acceptance scenarios.
- Business rules, state transitions, and transaction invariants.
- High-level development sequence.
- Agreed module ownership and MVP boundaries.
- A decision log for scope and architecture choices.
- A list of business rules and acceptance scenarios to carry into the detailed documents.

The requirements, use cases, business rules, architecture, database, API, security, and testing documents now form the initial planning baseline. They should become more precise alongside implementation rather than remaining speculative encyclopedias.

Exit criteria:

- The core order-to-completion flow is understood.
- Out-of-scope features are written down so they cannot silently expand the MVP.
- The user has explicitly approved moving from documentation to implementation.

### Phase 1: Project Foundation

Planned outcomes:

- Initialize the backend and frontend workspaces using the selected technology direction.
- Add local PostgreSQL through Docker Compose.
- Establish configuration profiles and environment-variable handling without committing secrets.
- Establish database migration conventions before creating application tables.
- Add a basic application health endpoint and a consistent API error envelope.
- Add the initial OpenAPI setup and project-level logging conventions.
- Add the test structure and a real PostgreSQL integration-test path.
- Add baseline formatting, static analysis, and build commands.

Verification:

- A clean checkout can start the local dependencies using the documented command.
- The backend can connect to PostgreSQL using the intended configuration.
- A migration can be applied to an empty database and the application can start against it.
- A failing request produces the documented error shape.

Do not add Redis, RabbitMQ, Kubernetes, or a service registry in this phase. None is required to prove that the foundation works.

### Phase 2: Identity and Access

Planned outcomes:

- User and role persistence.
- Secure password hashing and login endpoint.
- JWT issuance and protected-request authentication.
- Role-based authorization for representative operations.
- Account activation/deactivation and safe authentication failures.
- Security-focused API and service tests.

Verification:

- Unauthenticated requests cannot access protected resources.
- A valid token does not grant permissions beyond its role.
- Disabled users cannot authenticate or continue using a revoked account according to the documented token policy.
- Passwords and tokens are not written to logs.

Keep the first security implementation local and understandable. External identity providers, social login, and multi-tenant permissions are later concerns.

### Phase 3: Products, Materials, and BOM

Planned outcomes:

- Product and material master-data operations with DTO validation.
- Active/archive rules that prevent inactive items from being used in new production planning.
- BOM draft creation and activation.
- One active BOM revision per product.
- Single-level BOM lines with compatible units and positive quantities.
- A production-planning read model or service response that calculates requirements for a requested output quantity.

Verification:

- Invalid quantities, duplicate components, inactive references, and incompatible units are rejected.
- Activating a new BOM revision does not rewrite prior revisions.
- A production-order snapshot can be created from the active BOM and remains stable if the BOM later changes.

Do not add alternate materials, multi-level explosions, routing definitions, or automatic waste/yield calculations here. They are separate scope decisions, not prerequisites for the first workflow.

### Phase 4: Inventory and Reservations

Planned outcomes:

- Stock balance records for materials and finished products.
- Receipt and controlled-adjustment operations.
- Append-only inventory movement records with a reason and reference.
- Available-stock calculation using on-hand minus reserved quantity.
- Reservation, release, and consumption operations.
- Transaction boundaries and row-level concurrency protection for balance updates.

Verification:

- A receipt increases the correct balance and creates a movement record.
- An adjustment cannot create invalid negative stock unless an explicitly documented rule allows it.
- A reservation cannot exceed available stock.
- A multi-line reservation succeeds completely or changes nothing.
- Two concurrent reservation attempts cannot both claim the same available quantity.
- Releasing or consuming a reservation is idempotent or rejects duplicate commands according to the documented state rules.

This phase is the main persistence and concurrency milestone. It should be completed and tested before production orchestration is built on top of it.

### Phase 5: Customers and Customer Orders

Planned outcomes:

- Customer records and validation.
- Customer-order headers and lines.
- Order status transitions and cancellation rules.
- Product availability and quantity validation at the appropriate lifecycle boundary.
- A clear command or endpoint to select one confirmed order line for production.

Verification:

- A draft order can be corrected before confirmation.
- A confirmed order cannot be changed in ways that invalidate an already planned production order.
- Inactive products cannot be used for new confirmed orders.
- A production order cannot be created from an unconfirmed or already cancelled line.

Avoid adding shipping, invoicing, payment, or customer self-service in this phase. The order exists to create a traceable manufacturing demand.

### Phase 6: Production and Production Stages

Planned outcomes:

- Production order creation from one confirmed customer-order line.
- BOM snapshot and material-requirement records.
- Production-order lifecycle states.
- Material reservation command that uses the Inventory module boundary.
- Fixed ordered stage definitions and production-stage execution records.
- Validated start, progress, pause, and completion-preparation transitions.

Verification:

- Production creation fails without an active BOM.
- Requirement quantities are based on the captured BOM revision and requested production quantity.
- The same production order cannot reserve material twice.
- Stages cannot be skipped, repeated, or completed out of order.
- Cancelling a reserving production order applies the documented release behaviour.
- Cross-module calls use services or module interfaces rather than another module's repository.

The production module should coordinate the workflow, while Inventory remains the only owner of stock mutation and Production Stages remains the owner of stage transitions.

### Phase 7: Quality and End-to-End Completion

Planned outcomes:

- Final inspection records and defect records.
- Pass/fail quality outcome and completion gate.
- A completion service that coordinates material consumption, finished-product receipt, production status, and order status.
- Failure and rollback behaviour for the completion transaction.
- A complete API-level or service-level happy-path workflow test.

Verification:

- A passing inspection allows completion once all other prerequisites are satisfied.
- A failed inspection blocks successful completion and records the defects.
- Completion consumes exactly the reserved requirements and adds the expected finished quantity.
- Completion cannot be applied twice.
- A persistence failure during completion leaves no partial inventory or status update.
- The inventory movement history can explain the material and finished-product changes caused by completion.

This is the first point at which the project can claim the core business flow is implemented. Do not move to optional infrastructure before this phase is stable.

### Phase 8: API Contract and Thin Frontend

Planned outcomes:

- Complete OpenAPI descriptions for the implemented endpoints.
- Consistent pagination, filtering, validation, and error conventions where they are actually needed.
- A small React and TypeScript client with:
  - Login and current-user context
  - Product, material, and BOM setup views
  - Customer and customer-order workflow
  - Inventory balance and movement view
  - Production-stage progress view
  - Quality inspection view
- Loading, empty, validation, and authorization-error states.

Verification:

- The UI uses the documented API rather than bypassing service rules.
- A reviewer can complete the main workflow without direct database edits.
- Role restrictions are visible and enforced by the backend, not only hidden in the UI.
- The frontend remains a thin client; business calculations stay on the backend.

The frontend should be built from the stable workflow slices, not as a large dashboard before the API is reliable.

### Phase 9: Hardening and Portfolio Release

Planned outcomes:

- Complete unit, repository, integration, and workflow test coverage for important rules.
- Concurrency tests for reservations and any other contested state.
- Security review of authentication, authorization, input validation, secrets, and error disclosure.
- Dockerized local run instructions and safe sample data.
- Updated architecture, database, API, security, and testing documentation.
- Optional lightweight CI, such as GitHub Actions, if it runs the existing build and tests without adding unrelated complexity.
- A short demonstration scenario or screenshots only after the system is reproducible from a clean checkout.

Verification:

- A clean checkout follows the README successfully.
- Tests pass against the supported Java and PostgreSQL setup.
- The OpenAPI document matches the implemented endpoints.
- No secrets, local database files, or generated build artefacts are committed.
- The README explains the architecture, scope, setup, and known limitations honestly.

## 5. Cross-Cutting Definition of Done

No feature phase is complete until the feature has:

- A documented use case and business rule.
- A request/response contract where it is exposed through REST.
- Boundary validation and service-layer rule validation.
- A transaction boundary when multiple state changes must be atomic.
- Role checks appropriate to the operation.
- Unit tests for core rules.
- PostgreSQL-backed tests for persistence-sensitive behaviour.
- A consistent error path for expected failures.
- OpenAPI and Markdown documentation updated to match the behaviour.

This keeps the project from accumulating a large number of endpoints that are difficult to trust or explain.

## 6. Major Delivery Decisions

| Decision | Why it is appropriate | Alternative rejected | Interview and reengineering benefit |
| --- | --- | --- | --- |
| Build backend workflow slices before the full frontend | Business rules and transaction boundaries are the primary project goals | Build a dashboard first and fit the API around screens | Shows API-first thinking and reduces UI-driven domain leakage |
| Complete one end-to-end flow before broadening CRUD coverage | A tested order-to-completion path proves the architecture works across modules | Finish every master-data screen before connecting production | Produces an earlier demonstrable milestone and exposes integration problems sooner |
| Use package-by-module boundaries in one application | A solo developer can reason about one deployment while preserving ownership | Organize the entire backend only by technical layer or split into services | Makes future refactoring and possible service extraction more deliberate |
| Establish migrations before business tables | Schema history must be reproducible across local, test, and future deployment environments | Let Hibernate silently alter the shared schema | Demonstrates safe change management and avoids environment drift |
| Use PostgreSQL for persistence integration tests | Locking, constraints, and SQL semantics are part of the inventory design | Use only an in-memory database for speed | Demonstrates awareness of test fidelity and database-specific risk |
| Treat inventory reservation as a concurrency milestone | Stock correctness is more important than adding many inventory features | Add reservation as a simple read-then-write helper | Shows understanding of race conditions and transaction isolation |
| Defer optional infrastructure until a concrete need exists | The core workflow has no requirement for asynchronous messaging or distributed caching | Add Redis or RabbitMQ to make the project appear more enterprise-like | Shows technology judgment and avoids operational complexity without value |
| Keep the frontend intentionally small | A focused UI is enough to prove the API and workflow | Build a full ERP dashboard, reporting suite, and customer portal | Preserves time for backend tests, security, and design quality |
| Update design documents alongside implementation | Business rules and API contracts will evolve as edge cases are discovered | Write all documents once and allow code to diverge | Shows maintainability and makes future reengineering safer |

## 7. Scope Gates

The following gates prevent the project from expanding faster than it can be finished:

1. Do not add a new module unless it supports the core workflow or replaces a documented assumption.
2. Do not add a new infrastructure dependency unless the simpler in-process or PostgreSQL solution has been shown to be insufficient.
3. Do not add a frontend feature unless the corresponding backend use case and authorization rule are defined.
4. Do not mark a phase complete based only on a successful happy path; include its important rejection and rollback cases.
5. Do not broaden from one stock location, one active BOM per product, or fixed stages until the current model is tested and documented.
6. Record any deliberate scope change in the relevant Markdown document before implementing it.

## 8. Definition of MVP Complete

The MVP is complete when all of the following are true:

- A clean checkout can start the documented local environment.
- A user can authenticate and access only the operations allowed by the assigned role.
- Products, materials, and one active BOM revision can be maintained.
- Stock receipts, balances, reservations, releases, consumption, and movements are consistent.
- A confirmed customer-order line can produce one traceable production order.
- Material requirements are based on the BOM snapshot captured for that production order.
- Inventory reservations are safe under concurrent requests.
- Production stages enforce the documented sequence.
- A quality inspection and defect record can block or allow completion.
- Successful completion updates material and finished-product inventory atomically.
- The core API is documented with OpenAPI and the important workflow is covered by automated tests.
- The React client is sufficient to demonstrate the workflow but does not replace backend authorization or business rules.
- The README and detailed documents accurately describe what is implemented and what remains outside the MVP.

Only after this definition is met should Redis, RabbitMQ, advanced reporting, multi-location stock, or other extensions be reconsidered.
