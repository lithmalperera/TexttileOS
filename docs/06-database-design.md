# Database Design

**Project:** Textile Manufacturing Management System
**Document status:** Reduced MVP scope, proposed
**Implementation status:** Identity tables exist through migration `V2`; remaining schema is planned

## 1. Database Principles

- PostgreSQL is the source of truth.
- Flyway owns schema creation and evolution.
- Hibernate validates the migrated schema with `ddl-auto: validate`.
- UUIDs identify business records.
- Quantities use exact numeric types; Java uses `BigDecimal` for material quantities.
- Timestamps use UTC-aware types.
- Historical rows are archived or snapshotted instead of hard-deleted.
- Cross-row inventory rules are protected by locked service transactions.

## 2. Identity Tables

### `role`

Fixed seeded roles with UUID, code, name, and creation timestamp.

### `app_user`

Stores UUID, email, normalized email, display name, BCrypt password hash, status, timestamps, and optimistic-lock version.

### `user_role`

Join table with composite primary key `(user_id, role_id)` and restrictive foreign keys.

These tables already exist in `V2__identity_users_and_roles.sql`.

## 3. Catalog Tables

### `product`

Stores code, name, category, description, output unit, active/archive status, and timestamps.

### `material`

Stores code, name, material type, base unit, active/archive status, and timestamps.

### `bom`

Stores one active BOM per product. It contains product identity, status, and timestamps.

The MVP intentionally does not create a `bom_revision` table. A later revision system is documented as `EXT-002`.

### `bom_item`

Stores one material, unit, and quantity-per-product-unit for a BOM. A unique constraint prevents duplicate material lines within a BOM.

## 4. Inventory Tables

### `stock_location`

Stores the single seeded logical location, such as `MAIN`.

### `inventory_item`

Represents a product or material at a location. A check constraint requires exactly one of `material_id` or `product_id`.

### `stock_balance`

Stores on-hand quantity, reserved quantity, update time, and optimistic-lock version. The service enforces `reserved <= on_hand` while holding a row lock.

### `inventory_transaction`

Stores the operation type, reason, actor, timestamp, and business reference.

### `inventory_movement`

Stores append-only on-hand and reserved deltas for one inventory transaction and one inventory item.

### `material_reservation` and `material_reservation_line`

Store one production reservation and the requirement quantities it secures. The MVP allows one active reservation per production order.

## 5. Manufacturing Tables

### `manufacturing_order`

Stores:

- order number
- customer name and optional customer reference
- product identity and display snapshot
- positive quantity
- status: `CONFIRMED`, `IN_PRODUCTION`, `COMPLETED`, or `CANCELLED`
- timestamps and creator

There is no separate customer or order-line table in the MVP.

### `production_order`

Stores:

- production number
- unique manufacturing-order reference
- product identity and display snapshot
- planned quantity
- production status
- current fixed stage: `CUTTING`, `ASSEMBLY`, or `FINISHING`
- current stage status: `PENDING`, `IN_PROGRESS`, or `COMPLETED`
- stage and lifecycle timestamps
- version for stale updates

There is no separate stage-definition or stage-execution table in the MVP.

### `production_material_requirement`

Stores the immutable BOM snapshot: material identity, code/name snapshot, unit, quantity per product unit, and required quantity.

### `quality_inspection`

Stores one final inspection per production order, including result `PASS` or `FAIL`, defect count, notes, inspector, and timestamp.

There is no separate defect-record table in the MVP.

## 6. Relationships

```text
product 1 -------- 1 bom 1 -------- * bom_item * -------- 1 material
   |
   +-------- * manufacturing_order 1 -------- 1 production_order
                                                |
                                                +-------- * production_material_requirement
                                                |
                                                +-------- 0..1 material_reservation
                                                |
                                                +-------- 0..1 quality_inspection

material/product * -------- 1 inventory_item 1 -------- 1 stock_balance
                                              |
                                              +-------- * inventory_movement
```

## 7. Required Constraints

- normalized user email is unique
- product and material codes are unique
- one BOM exists per product in the MVP
- each BOM material is unique
- one manufacturing order has at most one production order
- one production order has at most one active reservation
- one production order has at most one quality inspection
- stock quantities are non-negative
- reserved quantity cannot exceed on-hand quantity after locked update
- defect count cannot exceed production quantity
- current stage transitions follow the fixed order
- historical records use restrictive foreign keys

## 8. Quantity and Time Conventions

- Material and stock values use `numeric(19,6)`.
- Finished-product quantities are positive whole numbers represented with scale zero.
- Timestamps use PostgreSQL `timestamptz` and UTC.
- Status values use strings with application constants and database checks.
- Product/material codes use normalized columns for case-insensitive uniqueness.

## 9. Migration Order

The planned migrations are:

1. `V1__baseline.sql`
2. `V2__identity_users_and_roles.sql` (already implemented)
3. Catalog tables: product, material, BOM, and BOM items
4. Inventory identity, balances, transactions, movements, and reservations
5. Manufacturing order, production order, requirements, and quality inspection
6. Indexes, constraints, and reference-data refinements

## 10. Deferred Schema Extensions

- `EXT-001`: customer and order-line tables
- `EXT-002`: BOM revision/effective-date tables
- `EXT-003`: stage definition and execution tables
- `EXT-004`: detailed defect and rework tables
- `EXT-005`: location, lot, serial, and transfer tables

The MVP model is deliberately designed so these can be added without changing the inventory ledger concept or completion transaction boundary.

## 11. Database Decisions

### One manufacturing-order table instead of customer/order tables

- **Decision:** store customer name/reference directly on a one-product manufacturing order.
- **Why:** keeps the traceability chain with fewer lifecycle rules.
- **Rejected:** normalized customer and multi-line order subsystem.
- **Benefit:** smaller relational model with an obvious later normalization path.

### Simple BOM instead of revision history

- **Decision:** one active BOM per product, snapshot on production creation.
- **Why:** the snapshot gives historical correctness without an engineering-change workflow.
- **Rejected:** draft/active/retired revisions in the MVP.
- **Benefit:** preserves the most valuable temporal rule at lower cost.

### Fixed stage columns instead of stage tables

- **Decision:** store current stage and stage status on production order.
- **Why:** fixed routes need state validation, not configuration storage.
- **Rejected:** configurable stage definitions and execution history.
- **Benefit:** clear state machine with fewer joins and migrations.
