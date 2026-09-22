# Business Rules

**Project:** Textile Manufacturing Management System  
**Document status:** Proposed  
**Implementation status:** Documentation only

This document defines the invariants and state transitions that protect the MVP domain. Controllers may validate input shape, but these rules must be enforced in the service layer and, where practical, supported by PostgreSQL constraints.

## 1. Rule Conventions

- Rule identifiers are stable references for service tests, integration tests, and API documentation.
- A rule applies regardless of whether a request comes from the React frontend or another approved API client.
- A rule that involves multiple records must specify its transaction boundary in the implementation design.
- Rules marked `MVP` are required before the first release. Rules marked `LATER` are intentionally deferred.

## 2. Shared Definitions

| Term | Meaning |
| --- | --- |
| Active | A record may be used by new business operations |
| Archived or retired | A record is preserved for history but cannot be used by new operations |
| On-hand quantity | Physical quantity recorded in the stock balance |
| Reserved quantity | On-hand quantity committed to an active production order but not yet consumed |
| Available quantity | `on-hand quantity - reserved quantity` |
| BOM snapshot | An immutable copy of the BOM revision and lines used by a production order |
| Full order-line production | The MVP creates one production order for the complete quantity of one confirmed customer-order line |
| Completion | The single transaction that consumes reserved materials, receives finished goods, and closes production |

## 3. Identity and Access Rules

| ID | Priority | Rule |
| --- | --- | --- |
| BR-IAM-001 | MVP | Login identifiers are unique under the documented normalization rule. |
| BR-IAM-002 | MVP | Passwords are stored only as strong one-way hashes and are never returned in DTOs or written to logs. |
| BR-IAM-003 | MVP | Every protected command requires an authenticated user and an appropriate role. |
| BR-IAM-004 | MVP | Deactivated users cannot start a new session. The token invalidation or expiry behaviour for already-issued tokens must be explicit in the security design. |
| BR-IAM-005 | MVP | Authorization is checked on the backend even when the frontend hides an unavailable action. |
| BR-IAM-006 | MVP | Authentication errors are generic enough not to reveal whether a user account exists. |
| BR-IAM-007 | MVP | Business records store the actor and timestamp for important state-changing operations where auditability is required. |

## 4. Catalog and Identity Rules

| ID | Priority | Rule |
| --- | --- | --- |
| BR-CAT-001 | MVP | Product and material codes are unique after trimming and applying the documented case-normalization rule. |
| BR-CAT-002 | MVP | Product and material names and codes must satisfy required length and character validation. |
| BR-CAT-003 | MVP | Archived products cannot be added to new customer orders, BOM revisions, or production orders. |
| BR-CAT-004 | MVP | Archived materials cannot be added to new BOM revisions or new stock receipts. Existing historical references remain readable. |
| BR-CAT-005 | MVP | Catalog records that are referenced by historical business records are archived rather than hard-deleted. |
| BR-CAT-006 | MVP | A product's output unit and a material's base unit are explicit; implicit unit assumptions are not allowed in calculations. |

## 5. Quantity and Unit Rules

The MVP should use a small, explicit unit set appropriate for cut-and-sew examples, such as `PIECE`, `METER`, and `KILOGRAM`. The final enumeration and database precision belong in the database-design document.

| ID | Priority | Rule |
| --- | --- | --- |
| BR-QTY-001 | MVP | Product output quantities are positive whole numbers because the MVP produces countable finished items. |
| BR-QTY-002 | MVP | Material and BOM quantities are positive decimal values represented with decimal-safe arithmetic. |
| BR-QTY-003 | MVP | A BOM line's unit must match the referenced material's base unit. Automatic unit conversion is not supported. |
| BR-QTY-004 | MVP | Required material quantity is calculated as `production quantity * BOM quantity per product unit`. No automatic waste or yield factor is applied. |
| BR-QTY-005 | MVP | Quantity precision and rounding are applied consistently at the service boundary and database boundary; silent binary floating-point calculations are prohibited. |
| BR-QTY-006 | LATER | Unit conversion, waste percentage, yield loss, and alternate unit-of-measure rules require a separate design decision and test set. |

## 6. BOM Rules

| ID | Priority | Rule |
| --- | --- | --- |
| BR-BOM-001 | MVP | A BOM belongs to exactly one product and has a revision identity. |
| BR-BOM-002 | MVP | A BOM revision must contain at least one material line to become active. |
| BR-BOM-003 | MVP | A BOM revision cannot contain the same material more than once. |
| BR-BOM-004 | MVP | Every referenced product and material must be active when a draft BOM is activated. |
| BR-BOM-005 | MVP | A product has at most one active BOM revision at any time. |
| BR-BOM-006 | MVP | Draft revisions are editable; active and retired revisions are immutable. A change creates a new draft revision. |
| BR-BOM-007 | MVP | Activating a new revision retires the previous active revision without changing its lines or historical metadata. |
| BR-BOM-008 | MVP | BOMs are single-level in the MVP. A BOM line references a material, not another product or nested BOM. |
| BR-BOM-009 | MVP | A production order stores the active revision identifier and an immutable copy of its lines at creation time. |
| BR-BOM-010 | MVP | Later BOM activation cannot change a production order's material requirements. |

## 7. Inventory Rules

### 7.1 Balance invariants

| ID | Priority | Rule |
| --- | --- | --- |
| BR-INV-001 | MVP | The MVP uses one logical stock location. Location transfers and multi-warehouse allocation are not supported. |
| BR-INV-002 | MVP | On-hand quantity cannot become negative through a normal receipt, adjustment, consumption, or completion command. |
| BR-INV-003 | MVP | Reserved quantity cannot be negative and cannot exceed on-hand quantity. |
| BR-INV-004 | MVP | Available quantity is always calculated as on-hand minus reserved quantity. |
| BR-INV-005 | MVP | A stock balance is identified by the stocked item and the logical location; duplicate balances for the same key are prohibited. |
| BR-INV-006 | MVP | Every mutation of on-hand or reserved quantity creates an append-only movement or reservation audit record. |
| BR-INV-007 | MVP | Inventory movement history is not edited or deleted as part of ordinary business operations. Corrections use a compensating movement. |

### 7.2 Receipts and adjustments

| ID | Priority | Rule |
| --- | --- | --- |
| BR-INV-008 | MVP | A receipt increases on-hand quantity and records item, quantity, type, actor, timestamp, reason, and reference. |
| BR-INV-009 | MVP | An adjustment requires an explicit reason and an authorized inventory role. |
| BR-INV-010 | MVP | An adjustment that would make on-hand or available stock invalid is rejected. |
| BR-INV-011 | MVP | A stock command updates the balance and its movement in one transaction. |

### 7.3 Reservations

| ID | Priority | Rule |
| --- | --- | --- |
| BR-INV-012 | MVP | A production order can have at most one active material reservation in the MVP. |
| BR-INV-013 | MVP | Reservation checks available quantity for every required material before changing any balance. |
| BR-INV-014 | MVP | Multi-line reservation is all-or-nothing. A shortage on one line prevents every line from being reserved. |
| BR-INV-015 | MVP | Reservation increases reserved quantity but does not reduce on-hand quantity. |
| BR-INV-016 | MVP | Concurrent reservation commands protect the same stock balance using a database transaction and row-level concurrency strategy. |
| BR-INV-017 | MVP | Competing balance rows should be locked in a consistent order to reduce avoidable deadlocks. |
| BR-INV-018 | MVP | A duplicate reservation command for a production order that already has an active reservation is rejected without additional movements. |

### 7.4 Release and consumption

| ID | Priority | Rule |
| --- | --- | --- |
| BR-INV-019 | MVP | Releasing a reservation reduces reserved quantity by the reserved amount and leaves on-hand quantity unchanged. |
| BR-INV-020 | MVP | Consuming a reservation reduces both on-hand and reserved quantities by the consumed amount. |
| BR-INV-021 | MVP | Production completion consumes only the requirement snapshot reserved for that production order. |
| BR-INV-022 | MVP | Successful completion increases finished-product on-hand quantity by the completed production quantity. |
| BR-INV-023 | MVP | Material consumption and finished-product receipt are committed together with production completion. |
| BR-INV-024 | MVP | The MVP records material consumption at successful completion. It does not model partial issue, scrap, or yield loss for a failed batch. |

## 8. Customer and Order Rules

| ID | Priority | Rule |
| --- | --- | --- |
| BR-ORD-001 | MVP | A customer must be active when a draft order is created and when an order is confirmed. |
| BR-ORD-002 | MVP | An order contains at least one line before confirmation. |
| BR-ORD-003 | MVP | An order line references one active product and a positive whole-unit quantity. |
| BR-ORD-004 | MVP | Draft orders can be edited; confirmation makes the demand stable for production planning. |
| BR-ORD-005 | MVP | A confirmed order line can produce at most one production order in the MVP. |
| BR-ORD-006 | MVP | The production quantity must equal the complete confirmed line quantity. Partial production is not supported. |
| BR-ORD-007 | MVP | A confirmed order cannot be changed in a way that invalidates a created production order. |
| BR-ORD-008 | MVP | An order cannot be cancelled after any of its lines has an associated production order. |
| BR-ORD-009 | MVP | An order becomes completed only when every line has completed production. |
| BR-ORD-010 | MVP | Historical orders retain their original product, customer, quantity, and status references even if catalog records are archived. |

### Customer-order status transitions

```text
DRAFT -> CONFIRMED -> IN_PRODUCTION -> COMPLETED
  |          |
  +--------> CANCELLED
```

Rules for the diagram:

- `DRAFT -> CONFIRMED` requires all order validation to pass.
- `DRAFT -> CANCELLED` is allowed before confirmation.
- `CONFIRMED -> CANCELLED` is allowed only when no production order exists for any line.
- `CONFIRMED -> IN_PRODUCTION` occurs when the first line receives a production order.
- `IN_PRODUCTION -> COMPLETED` occurs only when every line's production order is complete.
- A cancelled or completed order has no further ordinary status transitions.

## 9. Production and Stage Rules

### 9.1 Production-order status

The MVP uses the following production states and explicit transitions:

| From | To | Condition and side effect |
| --- | --- | --- |
| `PLANNED` | `MATERIALS_RESERVED` | All material requirements are reserved successfully. |
| `PLANNED` | `CANCELLED` | Cancellation is requested before reservation; no stock release is required. |
| `MATERIALS_RESERVED` | `IN_PROGRESS` | The first production stage starts. |
| `MATERIALS_RESERVED` | `CANCELLED` | Cancellation is requested before production starts; the active reservation is released. |
| `IN_PROGRESS` | `QUALITY_PENDING` | The final production stage is completed. |
| `QUALITY_PENDING` | `COMPLETED` | A final inspection has passed; completion consumes reserved materials and receives finished stock. |
| `QUALITY_PENDING` | `QUALITY_FAILED` | A final inspection has failed; no completion movement is created. |
| `QUALITY_FAILED` | `CANCELLED` | The failed order is closed without automated rework; the active reservation is released. |

No other production-state transitions are valid in the MVP. The transition table must be encoded in service tests and documented in the API specification.

| ID | Priority | Rule |
| --- | --- | --- |
| BR-PROD-001 | MVP | A production order belongs to exactly one confirmed customer-order line and one product. |
| BR-PROD-002 | MVP | A production order is created in `PLANNED` state with a BOM snapshot and calculated requirements. |
| BR-PROD-003 | MVP | A production order cannot be created without an active BOM. |
| BR-PROD-004 | MVP | A production order cannot reserve materials unless its requirements are complete and valid. |
| BR-PROD-005 | MVP | Reservation success moves the production order to `MATERIALS_RESERVED`. |
| BR-PROD-006 | MVP | Starting the first stage moves the order to `IN_PROGRESS`. |
| BR-PROD-007 | MVP | Completing the final stage moves the order to `QUALITY_PENDING`. |
| BR-PROD-008 | MVP | Only a quality-passed order in `QUALITY_PENDING` can move to `COMPLETED`. |
| BR-PROD-009 | MVP | A production order cannot move backwards or skip a required state. |
| BR-PROD-010 | MVP | A production order cannot be completed more than once. |
| BR-PROD-011 | MVP | Cancellation is allowed before production starts and after a failed quality result when the reservation can be safely released. |
| BR-PROD-012 | MVP | An in-progress production order is not cancelled by the MVP because physical material consumption and scrap are not modelled. |
| BR-PROD-013 | MVP | A quality-failed order cannot be completed; automated rework and reinspection are out of scope. It may be cancelled according to the documented cancellation rule. |

### 9.2 Fixed production route

Every MVP production order receives the following ordered stages:

1. `PREPARATION_CUTTING`
2. `SEWING_ASSEMBLY`
3. `FINISHING`

Quality inspection is a separate quality use case, not a production stage in this first model.

| ID | Priority | Rule |
| --- | --- | --- |
| BR-STG-001 | MVP | Stage records are created in the fixed order when the production order is planned. |
| BR-STG-002 | MVP | A stage begins in `PENDING`, then moves to `IN_PROGRESS`, then `COMPLETED`. |
| BR-STG-003 | MVP | Only the first incomplete stage can be started. |
| BR-STG-004 | MVP | Only the current `IN_PROGRESS` stage can be completed. |
| BR-STG-005 | MVP | A completed stage cannot be restarted or completed again. |
| BR-STG-006 | MVP | Completing the final stage is the only event that makes the order quality pending. |
| BR-STG-007 | LATER | Product-specific routes, parallel stages, skipped stages, and configurable workflow graphs require a separate design. |

## 10. Quality and Defect Rules

| ID | Priority | Rule |
| --- | --- | --- |
| BR-QLT-001 | MVP | A final inspection can be recorded only when every production stage is complete. |
| BR-QLT-002 | MVP | An inspection belongs to exactly one production order. |
| BR-QLT-003 | MVP | The MVP permits one final inspection outcome per production order. |
| BR-QLT-004 | MVP | A defect's affected quantity cannot be negative or greater than the production quantity. |
| BR-QLT-005 | MVP | A pass outcome is required before successful completion. |
| BR-QLT-006 | MVP | A fail outcome records the defects and blocks completion. |
| BR-QLT-007 | MVP | Quality records are immutable after submission in the MVP; a correction requires an explicitly documented future correction workflow. |
| BR-QLT-008 | LATER | Rework loops, repeat inspections, corrective-action plans, sampling plans, and statistical process control are deferred. |

## 11. Transaction and Consistency Rules

The following operations require explicit service-layer transactions:

| Operation | Atomic changes |
| --- | --- |
| BOM activation | Validate revision, retire prior active revision, activate new revision |
| Stock receipt or adjustment | Validate command, change balance, append movement |
| Material reservation | Lock balances, validate all requirements, update reservations, append movements, update production reservation state |
| Reservation release | Validate state, update reserved quantity, append release movements, update production state |
| Production completion | Validate quality and reservation, consume materials, receive finished goods, update reservation, production, and order state |
| Production cancellation | Validate cancellation state, release active reservation if present, append movements, update status |

Additional rules:

- A transaction must not commit a status change if its required related stock or audit mutation fails.
- A rollback must restore both business state and inventory balances/movements created by the failed operation.
- Cross-module orchestration can use services within the same transaction, but it must not bypass module ownership by writing another module's repository directly.
- The MVP has no external message broker. Cross-module consistency is provided by the shared PostgreSQL transaction boundary.
- Duplicate state-changing commands are rejected or handled idempotently according to the API contract; they must never create duplicate consumption or receipt movements.

## 12. Audit Rules

| ID | Priority | Rule |
| --- | --- | --- |
| BR-AUD-001 | MVP | Inventory movements are append-only and include item, movement type, quantity, reason/reference, actor, and timestamp. |
| BR-AUD-002 | MVP | Important state changes include the actor and timestamps where the operation is user initiated. |
| BR-AUD-003 | MVP | Historical production records retain the BOM revision and material snapshot used for planning. |
| BR-AUD-004 | MVP | Error responses and logs must not disclose passwords, JWT signing material, or unnecessary sensitive data. |
| BR-AUD-005 | SHOULD | Business references allow a reviewer to trace a movement back to its production order, receipt, adjustment, release, or completion. |

## 13. Rule Decision Log

| Decision | Why it is appropriate | Alternative rejected | Interview and reengineering benefit |
| --- | --- | --- | --- |
| Consume reserved material at successful completion | It keeps the MVP's inventory lifecycle atomic and avoids modelling partial issue and scrap | Record complex per-stage material issue and yield loss immediately | Makes the completion transaction easy to reason about while clearly documenting the realism boundary |
| Use full order-line production | It avoids remaining-quantity and split-order allocation rules | Allow arbitrary partial production in the first release | Shows how a simpler invariant can protect delivery without losing traceability |
| Use a single active BOM revision with immutable history | New production needs a stable definition while historical production remains explainable | Edit one BOM row in place or read the latest BOM dynamically | Demonstrates temporal data correctness and safe change management |
| Reserve all material lines atomically | A production order should not appear ready when only part of its input is secured | Allow partial reservation and manual shortage handling | Demonstrates transaction design and prevents misleading operational state |
| Use fixed production stages | The MVP needs state-transition depth without becoming a workflow engine | Configurable stage graphs and product-specific routing | Demonstrates explicit state modelling and gives a clean future extension seam |
| Disallow negative stock | Negative stock would hide reservation and consumption defects in the first release | Permit negative stock and reconcile later | Provides a simple, auditable inventory invariant suitable for testing |
| Preserve movements instead of editing history | Corrections remain explainable through compensating entries | Update or delete a mistaken movement | Demonstrates auditability and supports future reconciliation |
| Block cancellation after production starts | The MVP does not record physical partial consumption or scrap | Allow cancellation with ambiguous inventory effects | Prevents the system from claiming a false stock position |
| Keep quality inspection separate from production stages | Quality is a gate and record of outcome, not another fabrication step | Put inspection status into a generic stage without ownership rules | Clarifies module responsibilities and makes quality authorization explicit |
