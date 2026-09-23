# Requirements

**Project:** Textile Manufacturing Management System
**Document status:** Reduced MVP scope, proposed
**Implementation status:** Foundation complete; identity and access in progress

This document defines the smaller MVP. The original broad requirements remain available as deferred extensions in `docs/01-project-overview.md` and `feature-tracker/ideas.md`.

## 1. Requirement Notation

- **MUST:** required for the reduced MVP
- **SHOULD:** useful if it does not delay the core workflow
- **LATER:** explicitly deferred extension

## 2. Identity and Access Requirements

- `FR-IAM-01` MUST allow an administrator to create an internal user with email, display name, password, and one or more fixed roles.
- `FR-IAM-02` MUST hash passwords with BCrypt before persistence.
- `FR-IAM-03` MUST issue a short-lived JWT for valid credentials.
- `FR-IAM-04` MUST reject missing, invalid, and expired authentication.
- `FR-IAM-05` MUST enforce roles on the backend using endpoint and service checks.
- `FR-IAM-06` MUST allow an administrator to deactivate an internal user.
- `FR-IAM-07` MUST never return password hashes in API responses.
- `FR-IAM-08` SHOULD keep the fixed roles `ADMIN`, `PLANNER`, `INVENTORY_MANAGER`, `PRODUCTION_OPERATOR`, and `QUALITY_INSPECTOR`.

Deferred: public registration, customer authentication, password reset, OAuth, refresh-token persistence, and multi-tenant permissions.

## 3. Catalog Requirements

- `FR-CAT-01` MUST allow authorized users to create, view, update, and archive products.
- `FR-CAT-02` MUST allow authorized users to create, view, update, and archive materials.
- `FR-CAT-03` MUST enforce unique normalized product and material codes.
- `FR-CAT-04` MUST prevent archived products and materials from being used in new manufacturing work.
- `FR-BOM-01` MUST allow one active, single-level BOM per product.
- `FR-BOM-02` MUST validate positive material quantities and compatible base units.
- `FR-BOM-03` MUST reject duplicate material lines within a BOM.
- `FR-BOM-04` MUST calculate required material as production quantity multiplied by BOM quantity per product unit.
- `FR-BOM-05` MUST copy the active BOM lines into an immutable production snapshot.

Deferred: BOM revision approval, effective dates, alternative materials, multi-level BOMs, automatic waste, yield calculations, and unit conversion.

## 4. Inventory Requirements

- `FR-INV-01` MUST support one logical stock location.
- `FR-INV-02` MUST track material and finished-product on-hand quantity.
- `FR-INV-03` MUST expose available quantity as on-hand minus reserved quantity.
- `FR-INV-04` MUST record receipts and controlled adjustments.
- `FR-INV-05` MUST append an inventory movement for every balance or reservation mutation.
- `FR-INV-06` MUST reserve every required material line atomically or reserve none.
- `FR-INV-07` MUST protect competing reservations with a database transaction and row-level locking.
- `FR-INV-08` MUST release an unused reservation when an eligible production order is cancelled.
- `FR-INV-09` MUST consume reserved material and receive finished product during successful completion.
- `FR-INV-10` MUST reject invalid negative balances and reserved quantity greater than on-hand quantity.

Deferred: multiple locations, transfers, lots, serial numbers, expiry, barcode scanning, purchasing, and cost accounting.

## 5. Manufacturing Requirements

- `FR-MFG-01` MUST create a one-product manufacturing order containing customer name/reference, product, and positive quantity.
- `FR-MFG-02` MUST create at most one production order for a manufacturing order in the MVP.
- `FR-MFG-03` MUST create production from the active BOM and store a material requirement snapshot.
- `FR-MFG-04` MUST use fixed ordered stages: `CUTTING`, `ASSEMBLY`, and `FINISHING`.
- `FR-MFG-05` MUST reject skipped, repeated, or out-of-order stage transitions.
- `FR-MFG-06` MUST support a final quality inspection with pass/fail, defect count, and notes.
- `FR-MFG-07` MUST block completion after a failed quality inspection.
- `FR-MFG-08` MUST complete production only once and only after all material and quality prerequisites pass.
- `FR-MFG-09` MUST update material stock, finished-product stock, reservation state, production state, and manufacturing-order state atomically.

Deferred: customer master data, multi-line orders, partial production, scheduling, capacity planning, configurable routes, detailed rework, and machine integration.

## 6. API and Frontend Requirements

- `FR-API-01` MUST use request and response DTOs; persistence entities must not be returned directly.
- `FR-API-02` MUST validate request shape and business rules separately.
- `FR-API-03` MUST return one documented Problem Details-inspired error shape.
- `FR-API-04` MUST expose implemented endpoints through OpenAPI/Swagger.
- `FR-API-05` SHOULD provide bounded pagination for list endpoints that can grow.
- `FR-UI-01` MUST provide a small React client for login and the main manufacturing workflow.
- `FR-UI-02` MUST display loading, validation, authentication, authorization, and business-error states.
- `FR-UI-03` MUST keep calculations and authorization on the backend.

The UI does not need to provide every administrative CRUD operation. Swagger UI can exercise setup operations during the MVP demonstration.

## 7. Non-Functional Requirements

- `NFR-SEC-01` Passwords must be stored only as strong hashes.
- `NFR-SEC-02` Protected operations must be authorized by the server.
- `NFR-DATA-01` PostgreSQL constraints must support uniqueness, relationships, statuses, and quantity validity.
- `NFR-CON-01` Reservation and completion must have explicit transaction boundaries.
- `NFR-CON-02` At least one test must prove competing reservations cannot oversell stock.
- `NFR-TEST-01` Important workflows must run against real PostgreSQL through Testcontainers.
- `NFR-DOC-01` Documentation, API contract, tests, and implementation must remain aligned.
- `NFR-OPS-01` A clean checkout must run through Docker Compose and documented commands.
- `NFR-PERF-01` No production-scale SLA is claimed; avoid unbounded queries and obvious N+1 access.

## 8. MVP Acceptance Scenarios

- An administrator can create an internal user and assign a fixed role.
- A planner can create a product, materials, and an active BOM.
- An inventory manager can receive material and see movement history.
- A planner can create a one-product manufacturing order.
- Production stores a BOM snapshot that does not change when catalog data changes.
- An insufficient or concurrent reservation cannot create an invalid stock balance.
- An operator cannot skip a fixed stage.
- A failed quality result blocks completion.
- A passing result allows one atomic completion.
- The React client and Swagger UI can demonstrate the flow without direct database edits.

## 9. Scope Decisions

### Keep four logical areas

- **Decision:** Identity, Catalog, Inventory, and Manufacturing are the MVP areas.
- **Why:** These are enough to demonstrate the requested technologies and one complete business flow.
- **Rejected:** Ten separately developed business modules.
- **Benefit:** Less breadth, stronger implementation and explanation.

### Keep concurrency and completion

- **Decision:** Inventory locking and transactional completion remain mandatory.
- **Why:** They demonstrate the strongest backend engineering skills in this project.
- **Rejected:** A simple quantity update with no race-condition test.
- **Benefit:** Gives the project a technically memorable interview topic.

### Keep the order concept but remove order-management breadth

- **Decision:** A manufacturing order stores a customer name/reference, one product, and one quantity.
- **Why:** The business narrative still begins with customer demand.
- **Rejected:** A full customer and multi-line sales-order subsystem.
- **Benefit:** Keeps traceability without creating several extra lifecycles.
