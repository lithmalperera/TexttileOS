# System Architecture

**Project:** Textile Manufacturing Management System  
**Document status:** Proposed  
**Implementation status:** Documentation only

This document describes the planned application structure and runtime boundaries. It does not create or prescribe an implementation before the requirements and business rules are approved.

## 1. Architecture Summary

The system will be a modular monolith with one Spring Boot backend, one PostgreSQL database, and a small React client.

```text
+----------------------+        HTTPS/JSON        +--------------------------+
| React + TypeScript   | -----------------------> | Spring Boot REST API     |
| Vite client          | <----------------------- | Modular monolith        |
+----------------------+                          +------------+-------------+
                                                              |
                                                              | JDBC/JPA
                                                              v
                                                  +-----------+--------------+
                                                  | PostgreSQL              |
                                                  | One transactional DB    |
                                                  +--------------------------+
```

The backend is one deployable process, but it is divided into business modules with explicit ownership. The deployment boundary is intentionally simpler than the code boundary.

## 2. Architecture Goals

- Keep business responsibilities visible and separable inside one application.
- Keep transactionally related inventory and production operations in one database transaction.
- Prevent controllers and repositories from becoming the place where business workflows accumulate.
- Make the REST API independent of persistence entities and internal package details.
- Allow a future module to be extracted only if there is a demonstrated reason, without designing for distributed deployment prematurely.
- Make the system straightforward to run locally and inspect in a portfolio review.

## 3. Runtime Responsibilities

| Component | Responsibility | Not responsible for |
| --- | --- | --- |
| React client | Authentication interaction, workflow screens, form validation for usability, and API consumption | Authoritative business rules, authorization, material calculations, or stock mutation |
| REST API controllers | HTTP routing, DTO binding, boundary validation, and response mapping | Multi-step workflow decisions or direct persistence queries |
| Application services | Use-case orchestration, authorization checks, transaction boundaries, and business commands | Rendering UI or storing unrelated module data |
| Domain model | Module-owned invariants, state transitions, and persistence mappings | HTTP concerns or cross-module table updates |
| Repositories | Module-owned persistence queries and aggregate loading | Workflow orchestration or authorization policy |
| PostgreSQL | Durable storage, constraints, indexes, and transactional isolation | Authentication policy or UI behaviour |

## 4. Planned Backend Structure

The backend should use package-by-module boundaries. The following is a target shape, not a set of directories to create during the documentation phase.

```text
backend/
  src/main/java/.../
    common/
      error/
      security/
      web/
    identity/
      controller/
      dto/
      service/
      repository/
      domain/
    product/
      controller/
      dto/
      service/
      repository/
      domain/
    material/
    bom/
    inventory/
    customer/
    customerorder/
    production/
    productionstage/
    quality/
    workflow/
```

Each business module follows the default request path:

```text
Controller -> Service -> Repository -> PostgreSQL
```

The `workflow` package is a small application-level orchestration area. It owns no tables and is not a new business domain. It coordinates the few use cases that must cross multiple module boundaries, such as creating production from an order line and completing production after quality approval. This avoids putting a shared god service or circular module dependencies in the codebase.

The `common` package is restricted to genuinely cross-cutting technical concerns such as error mapping, security adapters, and web configuration. It must not become a place for shared business entities or arbitrary utility methods.

## 5. Module Ownership and Dependencies

| Module | Owns | May read through a module-facing service | Must not directly access |
| --- | --- | --- | --- |
| Identity and Access | Users, roles, credentials, account status | Authentication context needed by the application | Customer, order, or inventory repositories |
| Products | Product master data and lifecycle | Product summaries for BOMs, orders, and inventory | BOM or production tables |
| Materials | Material master data and lifecycle | Material summaries for BOMs and inventory | Inventory balances or reservations |
| BOM | BOM revisions and lines | Product and material validity | Inventory balances or production stage records |
| Inventory | Stock items, balances, movements, and reservations | Product/material identity needed to stock an item | BOM calculation or stage state |
| Customers | Customer master data | Customer validity for orders | User credentials or order persistence |
| Customer Orders | Orders, lines, and order lifecycle | Customer and product validation | Inventory mutation or stage execution |
| Production | Production orders and requirement snapshots | Order-line context, BOM snapshot, quality result | Direct updates to inventory or quality tables |
| Production Stages | Stage definitions and executions | Production-order context supplied by the workflow | Inventory and BOM persistence |
| Quality and Defects | Inspections and defect records | Production context supplied by the workflow | Direct production status or inventory updates |

### Cross-module dependency rules

1. A module may call a public application service or query interface exposed by another module.
2. A module may not call another module's repository directly.
3. A module may not mutate another module's entity or table through a shared persistence context.
4. DTOs or small read-only projections cross module boundaries; persistence entities do not.
5. The workflow orchestrator coordinates cross-module commands but delegates each mutation to the owning module.
6. A cross-module transaction uses the same PostgreSQL transaction because all modules are in one application.

## 6. Request and Command Flow

### Read request

```text
HTTP request
  -> authentication filter
  -> controller and request DTO validation
  -> module query service
  -> module repository
  -> response DTO
```

### Single-module command

```text
HTTP command
  -> authentication and authorization
  -> controller
  -> transactional module service
  -> domain validation/state transition
  -> repository changes
  -> response DTO
```

### Cross-module workflow command

```text
HTTP command
  -> controller
  -> workflow application service
  -> owning module services and query ports
  -> one database transaction where atomicity is required
  -> response DTO
```

For example, production completion validates the production aggregate and quality result, asks Inventory to consume the reservation and receive finished stock, then asks the owning services to update production and order state. No controller performs these steps itself.

## 7. Core Transaction Boundaries

The following operations must be transactional at the application-service boundary:

| Operation | Transaction responsibilities |
| --- | --- |
| BOM activation | Validate the draft, retire the previous active revision, and activate the new revision |
| Inventory receipt or adjustment | Validate the item and quantity, update balance, and append the movement |
| Material reservation | Lock relevant balances, validate every requirement, update reserved quantities, write reservation movements, and update reservation state |
| Reservation release | Validate state, reduce reserved quantities, write release movements, and update production state |
| Production completion | Validate stage and quality gates, consume reserved materials, receive finished goods, update reservation, production, and order state |
| Production cancellation | Validate cancellation state, release any active reservation, record the reason, and close production |

The default PostgreSQL isolation level can remain `READ COMMITTED` for normal work. Inventory reservation and consumption must lock the affected balance rows for update. The rows should be locked in a deterministic item order to reduce deadlocks. The application should not use serializable isolation for every request without evidence that the simpler boundary is insufficient.

## 8. State and Domain Ownership

The following records are treated as important aggregates or aggregate-like boundaries:

| Aggregate or boundary | Owner | Important invariant |
| --- | --- | --- |
| User account | Identity and Access | Account status and role assignment are valid |
| Product | Products | Code and lifecycle are valid |
| Material | Materials | Code, type, unit, and lifecycle are valid |
| BOM revision | BOM | One active revision per product and immutable active history |
| Stock balance | Inventory | Reserved quantity cannot exceed on-hand quantity |
| Customer order | Customer Orders | Confirmed demand and line lifecycle are stable |
| Production order | Production | BOM snapshot, requirement snapshot, and lifecycle are coherent |
| Stage execution | Production Stages | Stages progress in a fixed order |
| Quality inspection | Quality | Inspection is submitted once and gates completion |

An aggregate is not a reason to expose every child record through one endpoint. It is a boundary for protecting invariants and deciding which service owns a mutation.

## 9. Error Handling

Expected business failures should be represented by typed application errors and mapped by one shared REST exception handler. Examples include:

- Invalid request fields
- Duplicate product, material, or user codes
- Missing active BOM
- Insufficient available material
- Invalid state transition
- Unauthorized or forbidden operation
- Concurrent or stale command
- Missing referenced record

The error response must be stable and safe for clients. Stack traces, SQL statements, passwords, tokens, and internal implementation details must not be returned in production responses.

## 10. Persistence and Migration Strategy

PostgreSQL is the single source of truth for business state. Spring Data JPA and Hibernate provide persistence mapping and repository support, while versioned SQL migrations should own schema creation and evolution.

The planned approach is:

- Use Flyway or an equivalent versioned migration tool.
- Set Hibernate schema management to validate the migration-created schema rather than silently modifying it.
- Review schema migrations as source-controlled changes.
- Keep seed data for fixed roles and stage definitions separate from mutable demo data.
- Avoid database triggers for business workflows unless a later constraint cannot be enforced safely in the service and database together.

## 11. Local and Deployment Shape

### Development

Docker Compose should provide the local PostgreSQL dependency and the documented environment variables. The backend and frontend may run from their development tools during early work or be added as Compose services when that improves reproducibility.

### Portfolio demonstration

The expected demonstration shape is:

```text
React static/dev client -> Spring Boot API -> PostgreSQL
```

There is no requirement for Kubernetes, a service mesh, a message broker, a distributed cache, or multiple databases.

### Configuration boundaries

Configuration should distinguish:

- application environment and profile
- database connection
- JWT signing secret or key material
- allowed frontend origins
- logging level

Secrets belong in environment variables or a local ignored configuration file. They must not be placed in Markdown examples as real values.

## 12. Observability Expectations

- Log application startup and database migration status.
- Include a request or trace identifier in API errors and relevant logs.
- Include business references such as production-order identifiers in inventory workflow logs.
- Do not log credentials, bearer tokens, or full sensitive request bodies.
- Use inventory movements as the authoritative business audit trail for stock changes.
- Keep metrics limited to useful counters and timings unless a real performance problem justifies more infrastructure.

## 13. Architecture Decision Log

| Decision | Why it is appropriate | Alternative rejected | Interview and reengineering benefit |
| --- | --- | --- | --- |
| Use one deployable modular monolith | The core workflow needs local transactions and is manageable for one developer | Split each business area into a microservice | Shows boundary design without distributed operational cost |
| Organize by business module, then by Controller -> Service -> Repository | Makes ownership and responsibilities visible | Use one global technical layer for the whole backend | Makes changes easier to localize and future extraction more deliberate |
| Add a small workflow orchestration package with no tables | Cross-module commands need one place to coordinate without circular persistence dependencies | Put all orchestration in controllers or a shared god service | Demonstrates application-layer design while preserving module ownership |
| Use a shared PostgreSQL transaction for core workflows | Reservation and completion need atomic multi-module updates | Coordinate through asynchronous events before the core flow is stable | Demonstrates practical consistency and keeps failure analysis understandable |
| Lock inventory balances at the database boundary | Competing reservations must see a serialized balance decision | Use only an application-level availability check or a distributed lock | Demonstrates concurrency awareness using the system's source of truth |
| Use migration-controlled schema with JPA validation | Schema changes need reviewable history and reproducibility | Let Hibernate update a shared schema automatically | Demonstrates safe evolution and deployment discipline |
| Keep `common` technical and small | Shared business objects would weaken module ownership | Create a large shared kernel of entities and helpers | Makes coupling visible and limits future refactoring cost |
| Defer messaging and caching | The MVP has no measured asynchronous or high-read-throughput requirement | Add RabbitMQ or Redis at project start | Shows technology selection based on need rather than appearance |
