# Project Overview

**Project:** Textile Manufacturing Management System
**Document status:** Reduced MVP scope, proposed
**Implementation status:** Foundation complete; identity and access in progress

## 1. Purpose

This project demonstrates how a software engineer turns a manufacturing workflow into a maintainable REST API, relational model, transaction boundary, security model, and automated test suite.

The project is intentionally narrower than a full ERP system. The goal is not to model every department in a manufacturer. The goal is to build one complete, trustworthy workflow and explain every important design choice.

## 2. Recommended Domain Focus

The MVP represents a small-batch cut-and-sew manufacturer. The system uses generic product and material names, but its fixed manufacturing stages are:

1. `CUTTING`
2. `ASSEMBLY`
3. `FINISHING`

The MVP uses one logical warehouse and one product per manufacturing order. These constraints reduce business breadth without removing the important technical problems: BOM calculation, historical snapshots, stock reservations, concurrency, state transitions, quality gates, and atomic completion.

## 3. Core Workflow

1. An internal user authenticates with JWT.
2. A planner creates an active product, materials, and a simple active BOM.
3. An inventory manager records material stock.
4. A planner creates a manufacturing order containing a customer name/reference, one product, and one quantity.
5. The system creates a production order and copies the BOM into an immutable production snapshot.
6. Inventory checks availability and atomically reserves every requirement.
7. A production operator advances the production order through the fixed stages.
8. A quality user records a pass/fail result and optional defect count/notes.
9. A passing production order completes in one transaction: reserved materials are consumed and finished-product stock is received.

## 4. Four Logical Areas

### Identity and Access

Owns internal users, fixed roles, password hashes, JWT authentication, account status, and authorization.

The MVP keeps the existing administrator user API because it demonstrates DTOs, validation, BCrypt, service logic, and protected endpoints. It does not add password reset, self-registration, customer login, OAuth, refresh-token persistence, or a user-management frontend.

### Catalog

Owns:

- product code, name, category, output unit, and active/archive state
- material code, name, type, base unit, and active/archive state
- one simple active BOM per product
- BOM material lines and quantity-per-product-unit

The MVP does not implement a draft/active/retired BOM revision workflow. When production is created, the current BOM lines are copied into the production snapshot. A full revision system remains a documented extension.

### Inventory

Owns:

- one logical stock location
- material and finished-product stock identities
- on-hand, reserved, and available quantities
- receipts and controlled adjustments
- append-only movement history
- atomic reservation, release, and consumption
- finished-product receipt during completion

Inventory is the main backend engineering showcase. Reservations must use a database transaction and row-level locking so two concurrent requests cannot claim the same material.

### Manufacturing

Owns the complete operational flow after catalog setup:

- one-product manufacturing order with customer name/reference
- one production order per manufacturing order
- BOM and material requirement snapshot
- fixed stage state machine
- one final quality inspection with pass/fail and simple defect information
- transactional completion

The manufacturing area intentionally combines customer order, production, stages, and quality for the MVP. They remain clear concepts in the domain, but they do not need separate modules or complex lifecycles yet.

## 5. Actors

- **Administrator:** manages internal users and has full operational access.
- **Planner:** manages catalog data, BOMs, manufacturing orders, and production planning.
- **Inventory manager:** records stock and manages inventory commands.
- **Production operator:** advances fixed production stages.
- **Quality inspector:** records final quality outcomes.

Customers are represented by a name/reference on the manufacturing order. They are not authenticated users and are not a separate master-data module in the MVP.

## 6. MVP Inclusions

- Fixed internal roles and JWT authentication
- Product and material catalog
- Single-level active BOM per product
- BOM snapshot at production creation
- One warehouse/location
- Stock receipts, adjustments, balances, and movement ledger
- Atomic material reservations with PostgreSQL locking
- One-product manufacturing orders
- Production orders with fixed stages
- Simple quality pass/fail gate
- Transactional material consumption and finished-product receipt
- REST DTOs, validation, error envelope, and OpenAPI
- PostgreSQL integration tests, rollback tests, and one concurrency test
- Thin React workflow client
- Docker Compose development environment

## 7. Deferred Extensions

These are deliberately preserved as future extension points:

### EXT-001: Customer master and multi-line orders

Add `customer`, `customer_order`, and `customer_order_line` tables when the MVP needs reusable customers, multiple products per order, partial production, or richer order lifecycle rules.

### EXT-002: BOM revisions and engineering change control

Replace the single active BOM with immutable draft/active/retired revisions when historical BOM selection, approval, or engineering change history becomes necessary.

### EXT-003: Separate stage definitions and execution history

Extract fixed stage fields into stage-definition and stage-execution tables when product-specific routes, operator history, pause/resume, or configurable workflows are required.

### EXT-004: Detailed quality and rework

Add defect categories, defect records, dispositions, rework, and repeat inspections when a failed batch needs a managed corrective workflow.

### EXT-005: Advanced inventory

Add multiple locations, transfers, lots, serials, expiry, barcode support, or stock costing only after the one-location ledger and reservation model is stable.

### EXT-006: Procurement and planning

Supplier purchasing, MRP, capacity planning, scheduling, accounting, and machine integration remain outside the portfolio MVP.

## 8. Important Design Decisions

### Reduce ten areas to four logical areas

- **Decision:** Combine related business areas into Identity, Catalog, Inventory, and Manufacturing.
- **Why:** A solo developer can finish four coherent boundaries and still demonstrate modular design.
- **Rejected alternative:** Maintain ten separate MVP modules with independent lifecycles.
- **Benefit:** Shows scope control and preserves future extraction seams without building shallow features.

### Keep inventory concurrency as the technical centerpiece

- **Decision:** Keep atomic reservation, row locking, movement audit, and a concurrency test.
- **Why:** This is more valuable in an interview than many additional CRUD screens.
- **Rejected alternative:** Simplify inventory to a mutable quantity field.
- **Benefit:** Demonstrates transactions, isolation, constraints, and race-condition reasoning.

### Represent the customer order minimally

- **Decision:** Store customer name/reference, one product, and one quantity on a manufacturing order.
- **Why:** The business flow remains traceable without a customer master and multi-line allocation rules.
- **Rejected alternative:** Full customer and multi-line order lifecycles.
- **Benefit:** Keeps the order-to-production narrative while reducing tables and edge cases.

### Use fixed stages as a state machine

- **Decision:** Store the current fixed stage on the production order.
- **Why:** State-transition validation matters; configurable workflow storage does not belong in the MVP.
- **Rejected alternative:** Stage definition and execution tables with configurable routes.
- **Benefit:** Demonstrates explicit state modelling with a clear future extension.

### Keep the frontend thin

- **Decision:** Build only enough React UI to exercise the main workflow.
- **Why:** The project is primarily a backend engineering portfolio.
- **Rejected alternative:** Full ERP dashboard and master-data administration UI.
- **Benefit:** Protects time for security, concurrency, transactions, and testing.

## 9. Success Criteria

A reviewer should be able to:

1. Start PostgreSQL and the backend from the documented commands.
2. Authenticate as a role-appropriate internal user.
3. Create or inspect a product, materials, and an active BOM.
4. Record material stock.
5. Create a one-product manufacturing order.
6. Create production and inspect its BOM snapshot.
7. Reserve material and observe safe failure under insufficient/concurrent stock.
8. Advance the fixed stages without skipping.
9. Record quality pass/fail.
10. Complete passing production and inspect the resulting inventory movements.
