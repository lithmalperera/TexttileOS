# Database Design

**Project:** Textile Manufacturing Management System  
**Document status:** Proposed  
**Implementation status:** Documentation only

This document describes the planned relational model for PostgreSQL. It is a logical design, not a migration script. Exact column lengths, indexes, and migration ordering should be confirmed before implementation begins.

## 1. Database Principles

- PostgreSQL is the source of truth for business state.
- Every module owns its tables and exposes data to other modules through application services.
- Foreign keys protect relationships that are meaningful across the relational model.
- Business state is changed through service transactions, not ad hoc SQL from controllers.
- Historical records are preserved through immutable rows, status changes, and snapshots rather than destructive updates.
- Quantities use exact numeric types; Java business calculations use `BigDecimal` or integer quantities where appropriate.
- Timestamps are stored in UTC with timezone-aware database types.
- The schema is created and changed through versioned migrations. Hibernate validates the schema rather than owning schema evolution.

## 2. Identifier, Time, and Naming Conventions

| Concern | Proposed rule |
| --- | --- |
| Primary keys | UUID identifiers for business tables |
| Public identifiers | Expose UUIDs as strings in the API; do not expose database sequence details |
| Timestamps | PostgreSQL `timestamptz`, written and returned in UTC |
| Quantity values | `numeric(19,6)` for material and stock quantities; product counts must use scale zero |
| Text status values | `varchar` values with application constants and database checks where stable |
| Codes | Store normalized code/email values for case-insensitive uniqueness, while preserving display values when needed |
| Audit timestamps | `created_at`, `updated_at`, and operation-specific timestamps where relevant |
| Audit actor | Nullable `actor_user_id` only where a system action can legitimately have no user; user actions should identify the actor |
| Deletion | Prefer archive/retire status; use restrictive foreign keys for historical records |

UUIDs are appropriate for this portfolio system because identifiers are safe to expose through a REST API and do not require coordination between future application instances. A numeric sequence would be simpler and slightly smaller, but would expose ordering and create a less deliberate public identifier strategy.

## 3. Logical Relationship Overview

```text
product 1 -------- * bom_revision 1 -------- * bom_item * -------- 1 material
   |                                                        
   +-------- * customer_order_line * -------- 1 customer_order
   |
   +-------- * production_order 1 -------- * production_material_requirement
                         | 1
                         +-------- * production_stage_execution
                         | 1
                         +-------- 0..1 material_reservation 1 -------- * reservation_line
                         | 1
                         +-------- 0..1 quality_inspection 1 -------- * defect_record

material/product * -------- 1 inventory_item 1 -------- 1 stock_balance
                                              |
                                              +-------- * inventory_movement
```

The diagram is conceptual. `inventory_item` provides a relational stock identity for either a material or a finished product without making inventory movements reference arbitrary table names.

## 4. Table Catalogue

### 4.1 Identity and access tables

#### `app_user`

Stores internal users and authentication state.

Important fields:

- `id`
- `email` and normalized email value
- `display_name`
- `password_hash`
- `status` such as `ACTIVE` or `INACTIVE`
- `created_at`, `updated_at`
- `version` for stale-update detection where needed

Passwords are never stored in plaintext. Token claims are not persisted in this table.

#### `role`

Stores the fixed seeded roles: `ADMIN`, `PLANNER`, `INVENTORY_MANAGER`, `PRODUCTION_OPERATOR`, and `QUALITY_INSPECTOR`.

#### `user_role`

Associates users and roles. A unique constraint prevents duplicate assignments.

### 4.2 Product, material, and BOM tables

#### `product`

Stores product master data.

Important fields:

- `id`
- `code`, normalized code, `name`, `category`, `description`
- `output_unit`
- `status` such as `ACTIVE` or `ARCHIVED`
- `created_at`, `updated_at`, `archived_at`

#### `material`

Stores raw, consumable, or packaging material master data.

Important fields:

- `id`
- `code`, normalized code, `name`, `material_type`
- `base_unit`
- `status` such as `ACTIVE` or `ARCHIVED`
- `created_at`, `updated_at`, `archived_at`

#### `bom_revision`

Stores one revision of a product BOM.

Important fields:

- `id`
- `product_id`
- `revision_number`
- `status` such as `DRAFT`, `ACTIVE`, or `RETIRED`
- `created_by`, `created_at`, `activated_at`, `retired_at`
- optional revision note

#### `bom_item`

Stores the material lines belonging to a revision.

Important fields:

- `id`
- `bom_revision_id`
- `material_id`
- `quantity_per_product_unit`
- `unit`
- `created_at`

The material identity and quantity are copied into production requirements later. A BOM item cannot be edited after its parent revision is active.

### 4.3 Customer and order tables

#### `customer`

Stores business customer records.

Important fields:

- `id`
- `customer_code`
- `name`
- `email`, phone, and address fields appropriate to the MVP
- `status` such as `ACTIVE` or `INACTIVE`
- `created_at`, `updated_at`

#### `customer_order`

Stores a customer demand document.

Important fields:

- `id`
- `order_number`
- `customer_id`
- `status` such as `DRAFT`, `CONFIRMED`, `IN_PRODUCTION`, `COMPLETED`, or `CANCELLED`
- requested date and confirmation date
- `created_by`, `created_at`, `updated_at`
- customer display snapshot fields if historical display must not change when the customer record changes

#### `customer_order_line`

Stores the ordered product and quantity.

Important fields:

- `id`
- `customer_order_id`
- `product_id`
- `product_code_snapshot`, `product_name_snapshot`
- `ordered_quantity`
- `created_at`

The product foreign key preserves the relationship, while snapshot fields preserve the business description used at order time.

### 4.4 Inventory tables

#### `stock_location`

Represents the logical stock location. The MVP seeds one location, such as `MAIN`, but keeps the identity explicit so future multi-location work would not require changing every movement record.

Important fields:

- `id`
- `code`, `name`
- `status`

#### `inventory_item`

Represents a stockable product or material at a location.

Important fields:

- `id`
- `stock_location_id`
- `item_kind` such as `MATERIAL` or `PRODUCT`
- nullable `material_id`
- nullable `product_id`
- `unit`

A database check requires exactly one of `material_id` or `product_id` to be populated, and `item_kind` must agree with the populated reference. Unique constraints prevent duplicate stock identity at one location.

#### `stock_balance`

Stores the current balance for an inventory item.

Important fields:

- `id`
- `inventory_item_id` unique
- `on_hand_quantity`
- `reserved_quantity`
- `version`
- `updated_at`

Database checks enforce non-negative values. The service transaction enforces `reserved_quantity <= on_hand_quantity` while locking the row.

#### `inventory_transaction`

Represents one business inventory operation, such as a receipt, adjustment, reservation, release, consumption, or production receipt.

Important fields:

- `id`
- `transaction_type`
- `reference_type` and `reference_id` for a traceable business reference
- `reason`
- `actor_user_id`
- `occurred_at`

The reference pair is audit metadata rather than a polymorphic ownership relationship. The owning application service validates the reference. The actual stock relationship is held by movement lines.

#### `inventory_movement`

Stores append-only balance deltas belonging to an inventory transaction.

Important fields:

- `id`
- `inventory_transaction_id`
- `inventory_item_id`
- `on_hand_delta`
- `reserved_delta`
- optional resulting balance snapshot for audit readability

For example, a reservation has an on-hand delta of zero and a positive reserved delta. Consumption has negative deltas for both on-hand and reserved quantities. A transaction may have multiple movement rows, which allows a multi-line reservation to be audited as one operation.

### 4.5 Production and reservation tables

#### `production_order`

Stores the manufacturing execution record for one full customer-order line.

Important fields:

- `id`
- `production_number`
- `customer_order_line_id` unique
- `product_id`
- `product_code_snapshot`, `product_name_snapshot`
- `planned_quantity`
- `status` such as `PLANNED`, `MATERIALS_RESERVED`, `IN_PROGRESS`, `QUALITY_PENDING`, `QUALITY_FAILED`, `COMPLETED`, or `CANCELLED`
- `bom_revision_id`
- planned, started, completed, and cancelled timestamps as applicable
- cancellation reason
- `created_by`, `created_at`, `updated_at`, `version`

#### `production_material_requirement`

Stores the immutable material requirement snapshot for a production order.

Important fields:

- `id`
- `production_order_id`
- source `bom_item_id` where the historical relationship remains valid
- `material_id`
- `material_code_snapshot`, `material_name_snapshot`
- `unit`
- `quantity_per_product_unit`
- `required_quantity`

The production order calculates from these values rather than reading the current BOM during reservation or completion.

#### `material_reservation`

Stores the reservation lifecycle for a production order.

Important fields:

- `id`
- `production_order_id` unique in the MVP
- `status` such as `ACTIVE`, `RELEASED`, or `CONSUMED`
- created, released, or consumed timestamps
- `created_by`

#### `material_reservation_line`

Stores the material and quantity secured by the reservation.

Important fields:

- `id`
- `material_reservation_id`
- `production_material_requirement_id`
- `inventory_item_id`
- `reserved_quantity`

The line links the reservation to both the requirement snapshot and the concrete stock identity used.

### 4.6 Production-stage tables

#### `production_stage_definition`

Stores fixed stage definitions seeded for the MVP.

Example rows:

1. `PREPARATION_CUTTING`
2. `SEWING_ASSEMBLY`
3. `FINISHING`

Important fields:

- `id`
- `code`, `name`, `sequence_number`
- `status`

#### `production_stage_execution`

Stores one execution row for each stage of a production order.

Important fields:

- `id`
- `production_order_id`
- `stage_definition_id`
- sequence snapshot
- `status` such as `PENDING`, `IN_PROGRESS`, or `COMPLETED`
- started and completed timestamps
- start and completion actor identifiers where needed

The sequence is copied into the execution record so future definition changes cannot reorder an existing production order.

### 4.7 Quality tables

#### `quality_inspection`

Stores the final quality result for one production order.

Important fields:

- `id`
- `production_order_id` unique in the MVP
- `result` such as `PASS` or `FAIL`
- notes
- `inspected_by`
- `inspected_at`

#### `defect_record`

Stores defects associated with an inspection.

Important fields:

- `id`
- `quality_inspection_id`
- defect category
- affected quantity
- severity or disposition
- notes

## 5. Relationship and Constraint Strategy

### Required uniqueness

- normalized user email
- normalized product code
- normalized material code
- customer code and order number
- product plus BOM revision number
- one active BOM per product
- one material per BOM revision
- one production order per customer-order line
- one active reservation per production order
- one inventory balance per inventory item
- one final inspection per production order
- one stage sequence per production order

### Required checks

- quantities are positive where required
- on-hand and reserved quantities are non-negative
- reserved quantity does not exceed on-hand quantity after a locked transaction
- exactly one product or material reference is present on an inventory item
- status and unit values are from the documented set
- defect affected quantity is not greater than production quantity

### Foreign keys and deletion

- Historical business rows use restrictive foreign keys and are not hard-deleted through normal API operations.
- Product, material, customer, user, and BOM rows are archived or retired when they have historical references.
- Child rows belonging only to a draft aggregate may be removed by an explicit service command if the API allows it; this is not a general cascade-delete policy.
- User foreign keys on audit records should prevent deleting the user record.

## 6. Indexing Strategy

The initial migration should index:

- normalized product, material, customer, and user codes
- all foreign-key columns used in joins
- partial unique index for active BOM per product
- production order status and planned date
- customer order status and requested date
- stock balance inventory-item key
- inventory movements by inventory item and occurrence time
- inventory transactions by reference and occurrence time
- quality results and inspection time

Indexes should be justified by actual query patterns. The project should not add an index to every column by default.

## 7. Historical Data and Snapshots

The following snapshots protect the meaning of historical operations:

- Customer order lines store product code/name at order time.
- Production orders store product code/name and BOM revision identity.
- Production requirements store material code/name, unit, and quantity values used for planning.
- Stage executions store sequence information used for that production route.
- Inventory transactions store human-readable reason/reference metadata and actor/time.

Snapshots are not a replacement for foreign keys. The foreign key preserves relationship and referential integrity; the snapshot preserves the historical description shown to a reviewer.

## 8. Transaction and Migration Ordering

An initial migration sequence should follow dependency order:

1. Users, roles, and identity tables
2. Products, materials, and customers
3. Stock locations and inventory identity tables
4. BOM revisions and items
5. Customer orders and lines
6. Stage definitions
7. Production orders, requirements, and stage executions
8. Reservations and reservation lines
9. Quality inspections and defects
10. Inventory transactions and movements if their references require later tables, or use a migration-safe reference strategy
11. Indexes, checks, and seed data refinements

The exact ordering may change to handle foreign-key cycles, but every migration should leave the database in a usable state and should be safe to run against a clean database.

## 9. Database Design Decisions

| Decision | Why it is appropriate | Alternative rejected | Interview and reengineering benefit |
| --- | --- | --- | --- |
| Use PostgreSQL as the source of truth | Relational constraints and transactions match inventory and order relationships | Store business state in documents or application memory | Demonstrates relational modelling and consistency reasoning |
| Use UUID primary keys | Safe public identifiers and no dependence on globally coordinated sequences | Expose sequential numeric IDs | Shows deliberate API identity design, with a clear tradeoff in index size |
| Represent stock identity with `inventory_item` | One movement and balance model can support materials and finished products with real foreign keys | Put nullable material/product references on every movement or use an unvalidated item ID | Preserves relational integrity and gives a future location extension point |
| Maintain current balances plus an append-only movement ledger | Fast availability checks and explainable history are both needed | Recalculate all stock from movements or keep only a mutable balance | Demonstrates a practical audit and performance compromise |
| Snapshot BOM and descriptive values at order/production boundaries | Later master-data edits must not rewrite historical manufacturing meaning | Resolve current master data at every read | Demonstrates temporal correctness and auditability |
| Use status strings with checks rather than PostgreSQL native enums | New statuses can be introduced through migrations without type-management friction | Use unbounded strings or native enum types everywhere | Balances database validation with maintainable evolution |
| Use restrictive deletion and archive states | Historical manufacturing records must remain readable | Hard-delete referenced master data | Demonstrates lifecycle and referential-integrity discipline |
| Use numeric quantities and UTC timestamps | Material quantities require exact arithmetic and cross-system time consistency | Floating point quantities or local server time | Shows awareness of financial/manufacturing data precision and deployment differences |
| Keep one logical location but model the location identity | The MVP avoids warehouse complexity without making every table location-blind | Ignore location entirely or build full multi-warehouse allocation | Keeps the first schema small while making a later extension explicit |
| Enforce cross-row inventory invariants in locked service transactions | `reserved <= on_hand` cannot be protected by a simple column check alone | Rely only on application reads without locking | Demonstrates where database constraints end and transactional logic begins |
