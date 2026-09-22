# Project Overview

**Project:** Textile Manufacturing Management System  
**Document status:** Proposed  
**Implementation status:** Documentation only; application implementation has not started

## 1. Purpose

The Textile Manufacturing Management System is a portfolio project for modelling a small manufacturing operation from customer demand through production and quality approval. It is designed to show how a software engineer translates a real business workflow into a maintainable API, relational data model, transaction boundaries, and automated tests.

The system is not intended to replace a full enterprise resource planning platform. The project will focus on a narrow, demonstrable workflow with enough business rules to make the backend meaningful without creating an unfinishable collection of screens and integrations.

## 2. Recommended Domain Focus

The MVP should represent a small-batch cut-and-sew textile manufacturer. Example outputs could include finished garments or other sewn textile products made from fabric, thread, accessories, and packaging materials.

This focus is deliberate. Trying to represent spinning, weaving, dyeing, knitting, garment production, shipping, accounting, and supplier management in one first release would make the domain too broad for a solo project. A focused production route still demonstrates the important engineering problems: versioned product definitions, material requirements, stock reservations, workflow transitions, concurrency, and quality outcomes.

The names of products and materials remain general enough for the model to evolve later, but the initial seeded production stages should be appropriate for the chosen cut-and-sew workflow.

## 3. Goals

The project should demonstrate the following capabilities:

- Model related manufacturing concepts with explicit relational ownership and foreign-key integrity.
- Expose a REST API that uses request and response DTOs rather than serializing persistence entities directly.
- Apply validation at the API boundary and business-rule validation in services.
- Protect endpoints with JWT authentication and role-based authorization.
- Use service-layer transactions for state transitions and inventory changes.
- Prevent double reservation or overselling when concurrent requests target the same stock balance.
- Preserve an audit trail for inventory changes.
- Keep production orders historically correct when a product BOM changes later.
- Provide a small but usable React interface after the core API is stable.
- Document design decisions, API behaviour, security assumptions, and test strategy in a way that can be reviewed during an internship interview.

## 4. Actors

The MVP needs a small set of operational roles. A customer is a business record, not an authenticated user, in the first release.

| Actor or role | Main responsibilities |
| --- | --- |
| Administrator | Manage users, roles, and system-level master data |
| Planner | Manage products, BOM revisions, customer orders, and production planning |
| Inventory manager | Receive and adjust stock, inspect availability, and manage reservations |
| Production operator | Advance production stages and record production progress |
| Quality inspector | Record inspections, defects, and quality outcomes |
| Customer record | Represents the customer attached to an order; has no login in the MVP |

The final authorization matrix should be documented in `docs/08-security-design.md`. The role list is intentionally small; roles should not be created for every individual screen.

## 5. Proposed Module Boundaries

The ten business areas are logical modules inside one modular monolith. Each module owns its data and business rules. Modules communicate through application services or explicit module-facing interfaces, not by reaching into another module's repositories.

| Module | Owns | Does not own |
| --- | --- | --- |
| Identity and Access | User accounts, password credentials, roles, account status, JWT-related authentication concerns | Customer records or production ownership |
| Products | Product code/SKU, name, description, product category, output unit, active/archive state | BOM lines, stock quantities, or production progress |
| Materials | Material code, name, material type, base unit, active/archive state | On-hand quantity, reservations, or BOM revision state |
| Bill of Materials | BOM revisions and component lines, including material quantity per product unit | Material stock and production-stage execution |
| Inventory | Stock balances, available quantity, movement ledger, receipts, adjustments, reservations, release, and consumption | The formula used to calculate a product's material requirements |
| Customers | Customer identity and contact information | Login credentials, order status, or inventory |
| Customer Orders | Order headers, order lines, requested quantities, dates, and order lifecycle | Production stage progress or direct stock mutation |
| Production | Production orders, BOM/material-requirement snapshots, production lifecycle, and completion command | User authentication, raw stock rules, or quality inspection details |
| Production Stages | Controlled stage definitions and stage execution records with ordered transitions | BOM quantities, customer data, or inventory balances |
| Quality and Defects | Inspections, defect records, defect quantities, notes, and pass/fail outcomes | Direct stock mutation; quality gates production completion |

### Cross-module flow

The main orchestration should follow these boundaries:

1. A confirmed customer-order line is used to create a production order for one product and quantity.
2. Production asks the BOM module for the active revision and stores a snapshot of its lines on the production order.
3. Production calculates required quantities from the snapshot and asks Inventory to reserve them.
4. Production uses the Production Stages module to advance the order through its controlled route.
5. Quality records the final inspection and outcome.
6. A completion service coordinates the final transaction: consume the reserved material, record the finished-product receipt, and mark the production order complete only when the quality gate passes.

The exact transaction implementation belongs in the later architecture and database documents. The ownership rule is important now: cross-module orchestration may call another module's service, but it should not update that module's tables directly.

## 6. Internal Application Structure

The backend should use package-by-module boundaries. Within each module, the default request path is:

```text
Controller -> Service -> Repository
```

A planned module can contain supporting packages similar to:

```text
module/
  controller/
  dto/
  service/
  repository/
  domain/
```

The responsibilities are:

- **Controller:** HTTP routing, authentication context, request DTO binding, and boundary validation.
- **Service:** business rules, state transitions, cross-module orchestration, and transaction boundaries.
- **Repository:** persistence queries and aggregate retrieval; no workflow decisions.
- **DTO:** stable API contracts that prevent persistence details from becoming public API contracts.
- **Domain:** entities, value concepts, and module-owned persistence mappings.

Controllers should not contain inventory or production rules. Repositories should not be called directly by another module. Persistence entities should not be returned directly from REST endpoints.

## 7. MVP Boundary

### Included in the MVP

#### Identity and access

- Administrator-created local user accounts.
- Password hashing and login.
- JWT-based authentication.
- A small fixed role set with endpoint and service authorization.
- Account activation/deactivation.

#### Product, material, and BOM master data

- Create, view, update, archive, and validate products.
- Create, view, update, archive, and validate materials.
- Define a single-level BOM for a product.
- Support draft and active BOM revisions, with only one active revision per product.
- Require compatible units and positive component quantities.
- Copy the active BOM into a production-order material requirement snapshot when planning production.

#### Inventory

- Track raw-material stock and finished-product stock.
- Use one logical warehouse or stock location in the MVP.
- Record receipts and controlled adjustments.
- Maintain a current balance plus an append-only inventory movement history.
- Calculate available stock as on-hand quantity minus reserved quantity.
- Reserve all required material lines atomically, or reserve none of them.
- Release reservations when a production order is cancelled before consumption.
- Consume reserved materials and receive finished goods at successful production completion.

#### Customers and customer orders

- Maintain customer records.
- Create customer orders with one or more product lines.
- Validate product status and positive ordered quantities.
- Track a small order lifecycle such as draft, confirmed, in production, completed, and cancelled.
- Create one production order for the full quantity of one customer-order line at a time. Partial line splitting is outside the MVP, which keeps each production order tied to one product and one BOM.

#### Production and stages

- Create a production order for a confirmed order line.
- Calculate material requirements from the captured BOM snapshot.
- Track production-order status transitions.
- Use a fixed, ordered route for the initial cut-and-sew workflow, for example preparation/cutting, sewing or assembly, and finishing.
- Prevent skipping stages or applying duplicate transitions.
- Allow cancellation only in the documented pre-start or quality-failure states where reservation release is safe; cancellation after production starts is outside the MVP.

#### Quality and defects

- Record a final inspection for a production order.
- Record defect category, affected quantity, severity or disposition, and notes.
- Support a pass/fail outcome.
- Block successful production completion until the required quality gate passes.
- Keep automated rework, corrective-action plans, and statistical process control out of the MVP.

#### API, frontend, and engineering quality

- REST endpoints documented with OpenAPI/Swagger.
- Request validation and a consistent error response format.
- Role-protected endpoints.
- Unit tests for business rules.
- PostgreSQL-backed integration tests for persistence and workflow transactions.
- A small React interface for login, master-data setup, customer orders, inventory visibility, production progress, and quality inspection.
- Docker Compose for local PostgreSQL and application development.

### Explicitly out of scope for the MVP

| Area | Recommendation and reason |
| --- | --- |
| Microservices, service mesh, and Kubernetes | Remove from the MVP. They add deployment and distributed-consistency work without improving the core manufacturing demonstration. |
| Redis and RabbitMQ | Defer. Add them only if a measured caching or asynchronous integration requirement appears after the core workflow is stable. |
| GraphQL | Remove. REST is sufficient for the planned resources and better matches the requested API direction. |
| Procurement and suppliers | Defer. Stock receipts can be entered directly; supplier purchasing would introduce another large lifecycle. |
| Accounting, invoicing, payments, and costing | Remove from the MVP. These require financial rules that are not needed to demonstrate manufacturing control. |
| Payroll, attendance, and human-resources features | Remove. They are unrelated to the core workflow. |
| Multi-warehouse transfers | Defer. One logical stock location is enough to demonstrate reservation and transaction correctness. |
| Lots, serial numbers, expiry, barcode scanning, and warehouse optimization | Defer. They are useful extensions but create many additional data and process rules. |
| Multi-level or alternative BOMs | Defer. A single-level BOM with revision history is enough for the first complete flow. |
| MRP, demand forecasting, finite-capacity scheduling, and optimization | Remove from the MVP. A production order can be planned manually without building a planning engine. |
| Configurable workflow designers | Remove. Use a fixed, documented stage sequence rather than a workflow engine. |
| Machine telemetry, maintenance, and IoT integration | Defer. No external machine integration is required for the portfolio workflow. |
| Customer portal and self-service registration | Defer. Customers are internal master-data records in the MVP. |
| Automated rework and corrective-action management | Defer. Basic defects and a quality gate provide enough quality-domain depth initially. |
| Full ERP-style frontend | Remove. Build only the screens needed to exercise and explain the backend workflow. |
| Multi-tenancy and external identity providers | Defer. Local JWT authentication keeps the security model understandable and testable. |

These items are not rejected as permanently useless. They are rejected as first-release requirements because they would reduce the chance of completing and testing the core workflow.

## 8. Core Business Flow

The successful MVP path should be understandable from both the API and the UI:

1. An administrator creates users and assigns roles.
2. A planner creates an active product, active materials, and an active BOM revision.
3. An inventory manager records enough raw-material stock.
4. A planner creates and confirms a customer order.
5. A planner creates a production order from one confirmed order line.
6. The production service snapshots the active BOM and calculates material requirements.
7. Inventory checks available quantities and atomically reserves every required material line.
8. A production operator advances the order through the fixed production stages.
9. A quality inspector records the final inspection and defects, if any.
10. If quality passes, completion consumes the reserved materials, records the finished-product receipt, and marks production complete in one consistent transaction.

Important failure paths must be designed and tested as part of the same flow:

- Creating production without an active BOM is rejected.
- Insufficient stock causes an all-or-nothing reservation failure.
- A second concurrent reservation cannot use stock already reserved by the first successful request.
- Invalid or duplicate stage transitions are rejected.
- An unauthorised role cannot perform an operation outside its responsibility.
- A failed quality inspection cannot be silently treated as a completed production order.
- A failed completion transaction must not leave partial stock updates behind.

## 9. Major Design Decisions

Each decision below records the intended choice, the reason for it, the alternative intentionally not chosen for the MVP, and the portfolio or reengineering value it provides.

| Decision | Why it is appropriate | Alternative rejected | Interview and reengineering benefit |
| --- | --- | --- | --- |
| Focus on a small-batch cut-and-sew workflow | Gives the project a concrete manufacturing context and a manageable stage route | Model every textile process at once | Shows scope control and makes domain assumptions explainable |
| Use a modular monolith | Keeps deployment and debugging simple while preserving strong business boundaries | Microservices from the first commit | Demonstrates modular design without distributed transactions, service discovery, or operational overhead |
| Organize code by business module with Controller -> Service -> Repository inside each module | Keeps HTTP, business rules, and persistence responsibilities separate | One global controller/service/repository layer or a shared service for everything | Makes ownership visible and gives future extraction boundaries if scale ever requires it |
| Use PostgreSQL with JPA/Hibernate | Provides relational constraints, transactions, indexing, and realistic ORM work | A document database or an in-memory-only model | Demonstrates practical relational modelling and safe persistence behaviour |
| Use REST with DTOs and OpenAPI | Fits the React client, is easy to inspect, and makes API contracts explicit | GraphQL or exposing JPA entities directly | Shows contract design, validation, and separation between persistence and public API |
| Use local JWT authentication with a small role set | Matches the SPA/API split and demonstrates stateless authentication and authorization | Sessions for a server-rendered application or an external identity provider | Shows security fundamentals without outsourcing the main security flow or adding provider setup |
| Keep customer orders separate from production orders | A customer request and a manufacturing execution record have different lifecycles and ownership | One entity that represents both concepts | Preserves traceability and makes future partial production or rescheduling possible |
| Use active BOM revisions and snapshot the BOM into a production order | Historical production requirements must not change when a product definition changes | Read the current BOM every time production is viewed or completed | Demonstrates temporal correctness and protects auditability |
| Maintain stock balances plus an append-only movement ledger | Balances support fast availability checks while movements provide an audit trail | Only a mutable balance or recalculating every balance on every request | Demonstrates auditability, reconciliation thinking, and a clear basis for debugging inventory issues |
| Make reservation and completion transactional with row-level concurrency protection | Competing reservations must not oversell available material; related updates must succeed or fail together | Application-only checks, distributed locks, or an event-only inventory model | Demonstrates transaction boundaries, isolation concerns, and practical concurrency control |
| Use one logical stock location and a small set of base units | Preserves the important reservation logic without introducing warehouse-routing and conversion rules | Multi-warehouse stock, complex unit conversion, lot, and serial management | Shows deliberate scope reduction while leaving a clear extension path |
| Use a fixed ordered stage route | Gives production stages real state-transition rules without building a workflow product | User-configurable workflow graphs or a BPM engine | Demonstrates state modelling and validation without unnecessary framework complexity |
| Keep the frontend thin and build it after the core API slices | The project goal is backend and software-engineering depth, not a large dashboard | Frontend-first feature development | Protects project completion and makes the UI a consumer of a documented API |
| Use versioned database migrations from the first schema | Database changes should be reviewable and reproducible across environments; Flyway is a reasonable small supporting tool | Rely on `ddl-auto=update` as the shared schema process | Demonstrates deployment discipline and prevents silent schema drift |
| Test important persistence behaviour against PostgreSQL | Inventory locking and SQL constraints should be tested against the database that will be used | Rely only on an H2-style substitute | Exposes dialect and transaction differences early and provides stronger evidence of correctness |

The last two decisions are supporting practices, not changes to the requested technology direction. They complement Spring Data JPA and PostgreSQL rather than replacing them.

## 10. Non-Functional Expectations

The MVP should be evaluated against these practical expectations:

- **Correctness:** Invalid state transitions and insufficient stock are rejected consistently.
- **Consistency:** Reservation, consumption, and finished-stock receipt do not produce partial updates.
- **Security:** Passwords are never stored in plain text; protected operations require an appropriate role; secrets are configuration-driven.
- **Maintainability:** Module ownership and service boundaries are visible in the package structure.
- **Testability:** Business rules can be tested without HTTP, and key workflows can be tested through the real persistence layer.
- **Observability:** Errors are actionable, important state changes are logged appropriately, and inventory movements are queryable.
- **Usability:** A new reviewer can run the project locally, inspect the API, and complete the main workflow without reading the entire codebase.
- **Scope discipline:** Optional infrastructure is not added unless a concrete requirement and a simpler alternative have been evaluated.

## 11. Success Criteria

The MVP is successful when a reviewer can:

1. Start the documented local environment.
2. Authenticate as a role-appropriate user.
3. Create or inspect products, materials, and a BOM revision.
4. Record material stock.
5. Create a customer order and generate a production order for one line.
6. See calculated material requirements and reserve available material.
7. Observe stage progression and rejected invalid transitions.
8. Record a quality inspection with a defect outcome.
9. Complete a passing production order and see the corresponding inventory movements.
10. Read the OpenAPI contract, tests, and design documentation to understand why the system behaves this way.

The implementation should not begin with a broad collection of CRUD screens. The first meaningful milestone is one complete, tested, and documented happy path plus its most important failure paths.
