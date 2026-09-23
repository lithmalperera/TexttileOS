# Development Plan

**Project:** Textile Manufacturing Management System
**Document status:** Reduced MVP sequence
**Current phase:** Identity and access implementation

The plan now optimizes for a deep, explainable vertical slice rather than ten broad business modules.

## 1. Delivery Principles

- Build one modular monolith and one PostgreSQL database.
- Implement one small complete slice at a time.
- Let the developer run and understand every command.
- Keep inventory concurrency and completion rollback as non-negotiable depth areas.
- Use Swagger for setup while the React client remains intentionally small.
- Add no infrastructure unless a concrete requirement justifies it.
- Update the Learning Guide and feature tracker after each verified step.

## 2. Completed Foundation

Completed items:

- Git repository hygiene
- Spring Boot shell
- React/Vite shell
- Docker Compose PostgreSQL
- Flyway baseline, health, error contract, and OpenAPI
- Testcontainers PostgreSQL integration base

## 3. Reduced Implementation Sequence

### Phase 1: Identity and JWT

Finish `IAM-001`:

- identity migration
- JPA entities and repositories
- BCrypt service
- admin user DTOs and endpoints
- unit and API tests

Implement `IAM-002`:

- login endpoint
- JWT creation and validation
- current-user endpoint
- role checks
- disabled-user handling

Exit condition: a planner, inventory manager, operator, and quality user can authenticate and receive the correct authorization behaviour.

### Phase 2: Catalog and Simple BOM

Implement:

- product CRUD
- material CRUD
- one active BOM per product
- BOM calculation
- production requirement snapshot

Do not implement BOM revision approval, alternate materials, multi-level BOMs, or waste calculation.

Exit condition: Swagger can create catalog data and calculate the material requirement for a product quantity.

### Phase 3: Inventory Core

Implement:

- one stock location
- inventory item and balance
- receipt and adjustment commands
- append-only movement ledger
- available quantity
- atomic reservation and release
- row-level locking

Exit condition: one request can reserve material and two competing requests cannot oversell it.

### Phase 4: Manufacturing Flow

Implement:

- one-product manufacturing order with customer name/reference
- production order creation
- BOM snapshot and requirements
- fixed `CUTTING`, `ASSEMBLY`, `FINISHING` state machine
- one final quality inspection with pass/fail and defect count
- transactional completion

Exit condition: the complete workflow passes from manufacturing order to finished-product inventory.

### Phase 5: Thin React Client

Implement only the screens needed to demonstrate:

- login
- create or inspect a manufacturing order
- view inventory and reserve material
- advance stages
- submit quality
- complete production

Use Swagger for setup and administrative operations. Do not build a full ERP dashboard.

### Phase 6: Hardening and Portfolio Release

Complete:

- concurrency and rollback tests
- security tests
- API documentation examples
- seed/demo data
- Docker setup instructions
- README and Learning Guide updates
- optional lightweight CI

## 4. Features Removed from the Active MVP

The following original items are intentionally not active backlog work:

- separate customer master and customer-order modules
- multi-line orders and partial production
- separate production-stage module and execution history
- detailed defect records and rework
- BOM revision approval and engineering change management
- multi-warehouse, lots, serials, and barcodes
- procurement, accounting, costing, scheduling, and machine integration
- customer portal and external authentication
- notifications, Redis, RabbitMQ, GraphQL, Kubernetes, and microservices

These remain documented as extensions so the project can grow later without pretending they are MVP requirements.

## 5. Scope Gates

1. Do not add a new module unless it supports the core order-to-completion flow.
2. Do not add a new lifecycle when a field and a validated state transition are enough.
3. Do not add infrastructure before measuring a real need.
4. Do not expand the frontend while backend transactions or tests are incomplete.
5. Do not expand inventory until reservation and completion are proven under failure and concurrency.
6. Record any scope change before implementing it.

## 6. Definition of MVP Complete

The MVP is complete when:

- JWT login and roles work.
- Product, material, and simple BOM setup works.
- Material stock and movement history are consistent.
- Manufacturing orders create production orders with stable BOM snapshots.
- Reservations are atomic and concurrency-safe.
- Fixed stages reject invalid transitions.
- Quality failure blocks completion.
- Passing completion atomically consumes and receives stock.
- The API, tests, Docker setup, and React demonstration are documented.
