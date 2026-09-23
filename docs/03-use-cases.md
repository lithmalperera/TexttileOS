# Use Cases

**Project:** Textile Manufacturing Management System
**Document status:** Reduced MVP scope, proposed
**Implementation status:** Foundation complete; identity and access in progress

The use cases describe business outcomes rather than frontend pages. They are intentionally small enough to implement and test as vertical slices.

## 1. Actors

- **Administrator:** manages internal users and has full operational permissions.
- **Planner:** manages catalog data, BOMs, manufacturing orders, and production planning.
- **Inventory manager:** receives stock and performs inventory commands.
- **Production operator:** advances fixed manufacturing stages.
- **Quality inspector:** records final quality outcome.
- **Authenticated API client:** React, Swagger UI, or another approved client.

Customers are represented as fields on a manufacturing order. They are not application actors in the MVP.

## 2. Use-Case List

- `UC-01` Authenticate internal user
- `UC-02` Maintain products, materials, and simple BOM
- `UC-03` Receive and review inventory
- `UC-04` Create a manufacturing order
- `UC-05` Create production and snapshot BOM
- `UC-06` Reserve required materials
- `UC-07` Advance fixed production stages
- `UC-08` Record final quality result
- `UC-09` Complete production transactionally
- `UC-10` Review production and inventory traceability

## 3. Main Use Cases

### UC-01: Authenticate Internal User

1. The user submits email and password.
2. The backend verifies the BCrypt hash and active status.
3. The backend issues a short-lived JWT.
4. Later requests send the JWT in the Authorization header.

Failures:

- Invalid credentials return a generic authentication failure.
- Inactive users cannot authenticate.
- Missing or expired tokens are rejected.

### UC-02: Maintain Catalog and BOM

1. A planner creates or selects an active product.
2. The planner creates or selects active materials.
3. The planner defines one active BOM with positive material quantities.
4. The backend validates codes, units, active status, and duplicate lines.
5. The BOM becomes available for production planning.

Failures:

- Duplicate codes or BOM materials are rejected.
- Inactive references cannot be used.
- Incompatible units or non-positive quantities are rejected.

### UC-03: Receive and Review Inventory

1. An inventory manager records a receipt or adjustment.
2. The backend locks the balance for the command.
3. The balance changes and an append-only movement is recorded together.
4. The user can inspect on-hand, reserved, available, and movement history.

Failures:

- Invalid or negative stock is rejected.
- A failed balance update does not leave a movement without its balance change.

### UC-04: Create a Manufacturing Order

1. A planner enters customer name/reference, product, and quantity.
2. The backend validates that the product is active.
3. The order is stored as confirmed demand.

The MVP intentionally creates one-product orders directly. Draft editing, multi-line orders, and customer master records are deferred.

### UC-05: Create Production and Snapshot BOM

1. The planner selects a manufacturing order.
2. The backend confirms that it has not already been assigned to production.
3. The backend loads the current active BOM.
4. The backend creates a production order and copies the BOM lines into requirements.
5. The backend calculates required material quantities.

The snapshot remains unchanged if the product's BOM is edited later.

### UC-06: Reserve Required Materials

1. The planner or inventory manager requests reservation.
2. The backend locks all affected stock balances in deterministic order.
3. The backend checks every requirement against available quantity.
4. If every requirement is available, all reservations and movements commit.
5. If any requirement is short, nothing is reserved.

Failures:

- A competing request cannot oversell stock.
- A duplicate reservation command is rejected.

### UC-07: Advance Fixed Production Stages

1. A production operator starts the current stage.
2. The backend records the current stage state.
3. The operator completes the stage.
4. The backend exposes the next stage.
5. After `FINISHING`, production becomes quality pending.

The MVP uses fixed stage values rather than a configurable workflow engine.

### UC-08: Record Final Quality Result

1. A quality inspector opens a quality-pending production order.
2. The inspector records pass or fail, defect count, and notes.
3. A pass allows completion; a fail blocks completion.

There is one final inspection in the MVP. Rework and repeat inspection are deferred.

### UC-09: Complete Production Transactionally

1. The planner requests completion after all stages and quality checks pass.
2. The backend verifies the active reservation.
3. The backend consumes reserved materials.
4. The backend receives finished-product stock.
5. The backend closes production and manufacturing order state.
6. The transaction commits all changes together.

Failures:

- Completion without a passing quality result is rejected.
- A second completion is rejected.
- A persistence failure rolls back every stock and status change.

### UC-10: Review Traceability

An authorized user can trace a completed production order to:

- its manufacturing order
- its product and BOM snapshot
- material requirements
- reservation and consumption movements
- quality result
- finished-product receipt

## 4. Primary Demonstration

```text
UC-01 -> UC-02 -> UC-03 -> UC-04 -> UC-05
  -> UC-06 -> UC-07 -> UC-08 -> UC-09 -> UC-10
```

## 5. Deferred Use Cases

- Customer master and reusable customer records
- Multi-line customer orders and partial production
- Configurable production routes
- Rework and repeat inspection
- Supplier purchasing and planning
- Customer portal and external authentication

These remain possible future features without changing the core inventory and completion design.
