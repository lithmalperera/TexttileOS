# Business Rules

**Project:** Textile Manufacturing Management System
**Document status:** Reduced MVP scope, proposed
**Implementation status:** Foundation complete; identity and access in progress

These rules are authoritative for the reduced MVP. Controllers validate request shape, services enforce domain rules, and PostgreSQL constraints provide a final integrity boundary.

## 1. Shared Definitions

- **Active:** usable in new operations.
- **Archived:** readable for history but not usable in new operations.
- **Available:** `on_hand - reserved`.
- **Simple BOM:** one active single-level set of material lines for one product.
- **BOM snapshot:** immutable copy of the active BOM lines stored on production requirements.
- **Manufacturing order:** one customer name/reference, one product, and one positive quantity.
- **Production order:** the manufacturing execution record for one manufacturing order.

## 2. Identity Rules

- Login emails are normalized and unique.
- Passwords are stored only as BCrypt hashes.
- Every protected command requires an authenticated user with an appropriate role.
- Inactive users cannot start authentication.
- Role changes and important user state changes are attributable to an administrator.
- Fixed roles are `ADMIN`, `PLANNER`, `INVENTORY_MANAGER`, `PRODUCTION_OPERATOR`, and `QUALITY_INSPECTOR`.

## 3. Catalog Rules

- Product and material codes are unique after trimming and normalization.
- Archived products cannot be used in new manufacturing orders.
- Archived materials cannot be used in new BOMs or stock receipts.
- Referenced catalog records are archived rather than hard-deleted.
- Products use positive whole-number output quantities.
- Materials use positive decimal quantities.
- The MVP supports a small explicit unit set such as `PIECE`, `METER`, and `KILOGRAM`.
- A BOM line unit must match the material's base unit.
- A product has one active BOM in the MVP.
- A BOM contains at least one material line.
- A BOM cannot contain the same material twice.
- Required quantity is `production quantity * BOM quantity per product unit`.
- Production stores an immutable BOM snapshot; later catalog edits cannot change existing production requirements.

## 4. Inventory Rules

- The MVP has one logical stock location.
- On-hand and reserved quantities cannot be negative.
- Reserved quantity cannot exceed on-hand quantity.
- Every on-hand or reserved mutation creates an append-only movement.
- Corrections use compensating movements instead of editing history.
- Receipts and adjustments require an authorized inventory role.
- Reservation checks every requirement before changing any balance.
- Multi-material reservation is all-or-nothing.
- Competing reservations lock affected balance rows in deterministic order.
- A production order has at most one active reservation.
- Releasing a reservation reduces reserved quantity but not on-hand quantity.
- Consumption reduces both on-hand and reserved quantity.
- Successful completion receives finished-product stock in the same transaction as material consumption.
- The MVP records consumption at successful completion; it does not record partial issue or scrap.

## 5. Manufacturing Order Rules

- A manufacturing order contains one active product and one positive quantity.
- A customer name/reference is stored directly on the manufacturing order.
- A manufacturing order can create at most one production order.
- Manufacturing orders are created as confirmed demand; draft editing is outside the MVP.
- Manufacturing order state is `CONFIRMED`, `IN_PRODUCTION`, or `COMPLETED`.
- A completed manufacturing order cannot be changed.

## 6. Production Rules

Production states are `PLANNED`, `MATERIALS_RESERVED`, `IN_PROGRESS`, `QUALITY_PENDING`, `QUALITY_FAILED`, `COMPLETED`, and `CANCELLED`.

Allowed transitions:

- `PLANNED -> MATERIALS_RESERVED` requires complete reservation.
- `PLANNED -> CANCELLED` releases no reservation.
- `MATERIALS_RESERVED -> IN_PROGRESS` starts production.
- `MATERIALS_RESERVED -> CANCELLED` releases the reservation.
- `IN_PROGRESS -> QUALITY_PENDING` occurs after the final fixed stage.
- `QUALITY_PENDING -> QUALITY_FAILED` occurs after a failed inspection.
- `QUALITY_PENDING -> COMPLETED` requires a passed inspection and active reservation.
- `QUALITY_FAILED -> CANCELLED` closes a failed order without automated rework.
- No other transitions are valid.

Fixed stages are:

1. `CUTTING`
2. `ASSEMBLY`
3. `FINISHING`

The current stage and stage status are stored on the production order. A separate stage table is deferred.

## 7. Quality Rules

- Quality inspection is allowed only after `FINISHING` completes.
- The MVP has one final inspection per production order.
- An inspection records `PASS` or `FAIL`, defect count, and notes.
- Defect count cannot be negative or greater than the production quantity.
- A failed inspection blocks completion.
- Quality submissions are immutable in the MVP.
- Rework, defect categories, dispositions, and repeat inspections are deferred.

## 8. Transaction Rules

These operations require service-level transactions:

- Catalog/BOM update
- Stock receipt or adjustment
- Material reservation
- Reservation release
- Manufacturing order to production creation
- Stage transition
- Quality submission
- Production completion

Production completion must commit or roll back all of these together:

- material consumption
- finished-product receipt
- reservation state
- production state
- manufacturing-order state

## 9. Deferred Rules

The following rules are intentionally not part of the MVP:

- partial production and remaining order quantities
- multi-line order completion
- BOM effective dates and revision approval
- alternative or nested BOM components
- configurable stage routes
- physical material issue by stage
- scrap, yield, and rework accounting
- lot, serial, expiry, and warehouse transfer rules

## 10. Decision Rationale

### Simple order instead of customer-order subsystem

- **Decision:** Store customer name/reference directly on one-product manufacturing orders.
- **Why:** It preserves the business story while eliminating customer and order-line lifecycles.
- **Rejected:** Full customer master and multi-line order model.
- **Benefit:** Fewer tables and edge cases; the order-to-production trace remains clear.

### Current-stage fields instead of stage tables

- **Decision:** Store fixed stage and status on production order.
- **Why:** State validation is valuable; configurable route storage is not required.
- **Rejected:** Stage definition and execution history tables.
- **Benefit:** A small model still demonstrates state-machine design.

### Pass/fail quality instead of quality management

- **Decision:** One inspection with pass/fail, defect count, and notes.
- **Why:** It provides a real quality gate for completion.
- **Rejected:** Rework, corrective action, and statistical quality modules.
- **Benefit:** Keeps quality meaningful without creating another product.
