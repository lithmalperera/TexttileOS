# System Architecture

**Project:** Textile Manufacturing Management System
**Document status:** Reduced MVP scope, proposed
**Implementation status:** Foundation complete; identity and access in progress

## 1. Runtime Shape

The system is one Spring Boot modular monolith, one PostgreSQL database, and a thin React client.

```text
React + TypeScript
        |
        | HTTPS/JSON
        v
Spring Boot REST API
        |
        | JPA/JDBC
        v
PostgreSQL
```

The application has four logical areas. They are code boundaries inside one deployable process, not separate services.

## 2. Logical Areas

### Identity and Access

Owns users, roles, password hashes, account status, JWT authentication adapters, and authorization rules.

### Catalog

Owns products, materials, one active BOM per product, BOM lines, and requirement calculation.

### Inventory

Owns stock identities, balances, movement ledger, reservations, release, and consumption. No other area writes stock directly.

### Manufacturing

Owns manufacturing orders, production orders, requirement snapshots, fixed stage state, quality inspection, and completion orchestration.

The existing package placeholders for `customer`, `customerorder`, `productionstage`, and `quality` may remain physically present, but they are not independent MVP boundaries. Their MVP logic belongs to Manufacturing. They are future extraction points, not current features.

## 3. Planned Package Shape

```text
com.textile.manufacturing/
  common/
    error/
    security/
    web/
  identity/
    catalog/
      product/
      material/
      bom/
  inventory/
  manufacturing/
    order/
    production/
    quality/
  workflow/
```

The current skeleton has separate placeholder package names for some future areas. That is acceptable; package ownership should be consolidated or kept as subpackages when those features are implemented.

Every active area follows:

```text
Controller -> Service -> Repository -> PostgreSQL
```

The `workflow` package owns no tables. It coordinates cross-area actions such as production completion while delegating each mutation to the owning service.

## 4. Dependency Rules

- A logical area may call another area's public service or query interface.
- A logical area may not call another area's repository directly.
- Persistence entities do not cross area boundaries.
- DTOs and read-only projections cross boundaries.
- Inventory is the only owner of stock mutation.
- Manufacturing is the owner of production state and completion intent.
- Catalog is the owner of product, material, and BOM definitions.
- Controllers do not contain multi-step workflow logic.
- `common` contains technical concerns only, not shared business entities.

## 5. Request Flow

### Read

```text
HTTP request
  -> JWT/security filter
  -> controller and DTO binding
  -> query service
  -> repository
  -> response DTO
```

### Command

```text
HTTP command
  -> authentication and authorization
  -> controller
  -> transactional service
  -> domain validation/state transition
  -> repository changes
  -> response DTO
```

### Completion

```text
completion command
  -> Manufacturing workflow service
  -> validate quality and reservation
  -> Inventory consumes reserved material
  -> Inventory receives finished product
  -> Manufacturing closes production and order
  -> one PostgreSQL transaction commits
```

## 6. Transaction Boundaries

The following operations are transactional:

- BOM update
- Stock receipt or adjustment
- Material reservation
- Reservation release
- Manufacturing-order to production creation
- Fixed-stage transition
- Quality submission
- Production completion

Inventory reservation and consumption lock affected balance rows in deterministic order. Normal PostgreSQL `READ COMMITTED` isolation is sufficient for the MVP; global serializable isolation is unnecessary.

## 7. Security Boundary

The browser is untrusted. It cannot authoritatively calculate requirements, authorize users, change stock, or set production status. The API performs all validation and authorization.

Public endpoints are limited to health and API documentation. Business endpoints require JWT authentication once `IAM-002` is complete.

## 8. Deferred Architecture Extensions

The reduced architecture leaves future seams for:

- a Customer module with reusable customer records and multi-line orders
- a BOM revision module with engineering change control
- a Production Stage module with configurable routes and execution history
- a Quality module with defect records and rework
- multiple inventory locations and transfer workflows

These are intentionally not separate services now. A future extraction should be driven by deployment or scaling evidence, not by the number of nouns in the domain.

## 9. Architecture Decisions

### Four logical areas instead of ten deployed-like modules

- **Decision:** group related MVP concepts into four areas.
- **Why:** reduces coordination and lets one developer finish a deep vertical slice.
- **Rejected:** one independently designed module for every business noun.
- **Benefit:** clear ownership without pretending a small project needs distributed architecture.

### Shared transaction instead of events

- **Decision:** use one PostgreSQL transaction for reservation and completion.
- **Why:** all data lives in one application and database.
- **Rejected:** introduce RabbitMQ or eventual consistency before a requirement exists.
- **Benefit:** easier rollback reasoning and a strong demonstration of ACID consistency.

### Fixed stages instead of a workflow engine

- **Decision:** store fixed stage state on production.
- **Why:** state-transition validation is valuable; workflow configuration is not needed.
- **Rejected:** configurable route graphs or BPM tooling.
- **Benefit:** small model, clear tests, obvious future extension.

### Thin client after backend slices

- **Decision:** implement React after the API workflow is stable.
- **Why:** the project demonstrates backend engineering first.
- **Rejected:** frontend-first development and a full ERP dashboard.
- **Benefit:** reduces rework and keeps business rules on the server.
