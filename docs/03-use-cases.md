# Use Cases

**Project:** Textile Manufacturing Management System  
**Document status:** Proposed  
**Implementation status:** Documentation only

These use cases describe the business actions a user or system performs. They are intentionally larger than individual CRUD endpoints so that the design stays focused on outcomes and rules rather than screens. Endpoint-level contracts will be defined later in `docs/07-api-specification.md`.

## 1. Actors

| Actor | Description |
| --- | --- |
| Administrator | Manages internal accounts, roles, and system-level master data |
| Planner | Defines products, materials, BOMs, customers, orders, and production demand |
| Inventory manager | Records stock and performs inventory commands |
| Production operator | Advances a production order through its fixed stage route |
| Quality inspector | Inspects output and records defects and quality outcome |
| Authenticated API client | Represents the React frontend or an approved API consumer |
| PostgreSQL-backed system | Persists state, enforces relationships, and participates in transactions; it is not an independent business actor |

Customers are represented as records used by planners. Customers do not authenticate in the MVP.

## 2. Use-Case Catalogue

| ID | Use case | Primary actor | Priority | Main modules |
| --- | --- | --- | --- | --- |
| UC-01 | Authenticate user | Any internal user | MUST | Identity and Access |
| UC-02 | Maintain product and material catalog | Planner or Administrator | MUST | Products, Materials |
| UC-03 | Create and activate a BOM revision | Planner | MUST | Products, Materials, BOM |
| UC-04 | Record and review inventory | Inventory manager | MUST | Inventory, Materials, Products |
| UC-05 | Create and confirm customer order | Planner | MUST | Customers, Customer Orders, Products |
| UC-06 | Plan production from an order line | Planner | MUST | Customer Orders, Production, BOM |
| UC-07 | Reserve production materials | Planner or Inventory manager | MUST | Production, Inventory |
| UC-08 | Execute production stages | Production operator | MUST | Production, Production Stages |
| UC-09 | Inspect production quality | Quality inspector | MUST | Quality, Production |
| UC-10 | Complete a quality-approved production order | Planner | MUST | Production, Quality, Inventory, Customer Orders |
| UC-11 | Cancel production and release a reservation | Planner or Inventory manager | MUST | Production, Inventory |
| UC-12 | Review workflow and inventory traceability | Authorized internal user | SHOULD | All relevant read models |

## 3. Detailed Use Cases

### UC-01: Authenticate User

**Primary actor:** Any internal user  
**Trigger:** The user submits a login identifier and password.  
**Preconditions:** The account exists and is active.  
**Related requirements:** FR-IAM-02 through FR-IAM-06

**Main success scenario:**

1. The client sends credentials over the protected transport expected by the deployment.
2. The system validates the credentials without revealing sensitive details.
3. The system issues a JWT containing the documented identity and authorization claims.
4. The client stores the token according to the frontend security design and uses it for protected requests.
5. The system returns the authenticated user's display information and effective roles when required by the client.

**Alternative and exception flows:**

- Invalid credentials produce a generic authentication failure.
- A deactivated account cannot start a session.
- An expired or invalid token is rejected on a protected request.
- A role that lacks permission receives an authorization failure rather than a successful state change.

**Postconditions:** The user has an authenticated client session represented by the documented JWT policy, or no session is created.

### UC-02: Maintain Product and Material Catalog

**Primary actor:** Planner or Administrator  
**Trigger:** The actor needs to create, correct, inspect, or archive a product or material.  

**Main success scenario:**

1. The actor submits the required master-data fields.
2. The system validates required values, code uniqueness, status, and unit format.
3. The system stores the product or material as active unless the command explicitly creates an archived record.
4. The system returns a DTO containing the stable identifier and current state.
5. The actor can later update permitted descriptive fields or archive the item.

**Alternative and exception flows:**

- A duplicate code is rejected without changing the existing record.
- An invalid unit or missing required value is rejected at the API boundary.
- An item referenced by historical or active business records is not hard-deleted.
- An archived item remains readable in historical records but cannot be used for new BOMs, orders, or production planning.

**Postconditions:** The catalog contains a valid, traceable product or material state.

### UC-03: Create and Activate a BOM Revision

**Primary actor:** Planner  
**Trigger:** The planner needs to define or change the materials required for a product.  

**Main success scenario:**

1. The planner creates a draft revision for an active product.
2. The planner adds one or more material lines with positive quantity-per-product-unit values.
3. The system validates duplicate components, material status, and unit compatibility.
4. The planner submits the draft for activation.
5. The system activates the revision and retires the previous active revision, if one exists.
6. The system preserves the revision history and returns the active revision.

**Alternative and exception flows:**

- An empty BOM cannot be activated.
- A draft with duplicate material lines or incompatible units is rejected.
- An active or retired revision cannot be edited in place.
- A material archived after an older revision was used does not rewrite that revision.

**Postconditions:** Exactly one valid active BOM revision exists for the product, and prior revisions remain historically readable.

### UC-04: Record and Review Inventory

**Primary actor:** Inventory manager  

**Main success scenario:**

1. The inventory manager selects an item and enters a positive receipt or an approved adjustment.
2. The system validates the quantity, item status, reason, and reference.
3. The system updates the stock balance in a transaction.
4. The system appends an inventory movement containing the item, quantity, type, reason, actor, and timestamp.
5. The inventory manager can view on-hand, reserved, available, and movement history.

**Alternative and exception flows:**

- A receipt for an unknown or prohibited item is rejected.
- An adjustment that would violate the no-negative-stock rule is rejected unless a future rule explicitly permits it.
- A duplicate command is rejected or handled idempotently according to the API contract.
- A persistence failure rolls back both the balance and movement.

**Postconditions:** The balance and its audit movement agree, or neither is changed.

### UC-05: Create and Confirm Customer Order

**Primary actor:** Planner  

**Main success scenario:**

1. The planner creates or selects an active customer.
2. The planner creates a draft order and adds one or more active product lines.
3. The system validates positive whole-unit quantities and product status.
4. The planner confirms the order.
5. The system stores the confirmation time and makes each line eligible for one production order.

**Alternative and exception flows:**

- A draft order can be corrected before confirmation.
- An inactive customer or product prevents confirmation.
- An order with no lines or invalid quantity cannot be confirmed.
- A confirmed order cannot be edited in a way that invalidates an existing production order.
- An order cannot be cancelled after any line has entered production.

**Postconditions:** A confirmed order represents stable manufacturing demand.

### UC-06: Plan Production from an Order Line

**Primary actor:** Planner  

**Main success scenario:**

1. The planner selects one confirmed order line.
2. The system verifies that the line has not already been assigned to a production order.
3. The system loads the active BOM revision.
4. The system creates a production order for the full line quantity.
5. The system copies the BOM revision and lines into a production-specific snapshot.
6. The system calculates and stores the required material quantities.
7. The system creates the fixed production-stage records in order.
8. The order line becomes associated with the new production order.

**Alternative and exception flows:**

- No active BOM causes the command to fail without creating a partial production order.
- A second request for the same line is rejected by the business rule and database constraint strategy.
- A BOM changed after this operation does not alter the production snapshot.
- A product or order line that became inactive or cancelled is rejected.

**Postconditions:** A production order in the planned state exists with stable requirements and stages.

### UC-07: Reserve Production Materials

**Primary actor:** Planner or Inventory manager  

**Main success scenario:**

1. The actor requests reservation for the production order.
2. The system locks the relevant stock balances in a safe and consistent order.
3. The system calculates available quantity for every requirement.
4. The system rejects the whole command if any requirement is insufficient.
5. If all requirements are available, the system increases reserved quantity for every line.
6. The system records reservation movements and the production-order reservation state in one transaction.
7. The production order moves to the material-reserved state.

**Alternative and exception flows:**

- Insufficient material results in no partial reservation.
- A concurrent reservation waits or fails according to the database transaction policy and cannot oversell stock.
- A duplicate reservation request is rejected because the production order already has an active reservation.
- A stock or persistence error rolls back every balance and reservation change.

**Postconditions:** Either the production order has a complete active reservation or the system is unchanged by the command.

### UC-08: Execute Production Stages

**Primary actor:** Production operator  

**Main success scenario:**

1. The operator starts the first stage.
2. The system records the start time and marks the stage in progress.
3. The operator completes the current stage.
4. The system records the completion time and exposes the next stage.
5. The operator repeats the start and completion actions for each remaining stage.
6. On completion of the final stage, the system moves the production order to quality pending.

**Alternative and exception flows:**

- An operator cannot start a stage before material reservation.
- A stage cannot start before the preceding stage is complete.
- A completed stage cannot be completed again.
- A user without the production-operator permission cannot change stage state.
- A duplicate request is rejected without changing timestamps or status.

**Postconditions:** All production stages are complete and the order awaits quality inspection.

### UC-09: Inspect Production Quality

**Primary actor:** Quality inspector  

**Main success scenario:**

1. The quality inspector opens the production order and records inspection details.
2. The inspector records zero or more defects with category, quantity, severity or disposition, and notes.
3. The inspector selects pass or fail.
4. The system validates that affected quantities are sensible for the production quantity.
5. The system stores the inspection and defects with actor and timestamp.
6. A pass leaves the order eligible for completion; a fail changes it to quality failed and blocks completion.

**Alternative and exception flows:**

- An inspection cannot be created before the final production stage is complete.
- A second final inspection is rejected under the MVP one-inspection rule.
- A defect quantity greater than the produced quantity is rejected.
- A user without quality permission cannot record or alter the inspection.

**Postconditions:** The production order has a recorded quality outcome and the completion gate can evaluate it.

### UC-10: Complete a Quality-Approved Production Order

**Primary actor:** Planner  

**Main success scenario:**

1. The actor requests production completion.
2. The system verifies the production state, quality outcome, active reservation, and requirement snapshot.
3. The system consumes each reserved material quantity.
4. The system records the finished-product receipt for the produced quantity.
5. The system marks the reservation consumed and the production order completed.
6. The system updates the related order line and customer-order state when all lines are complete.
7. The transaction commits and the API returns the completed result.

**Alternative and exception flows:**

- A failed inspection rejects completion without changing inventory.
- A missing or already released reservation rejects completion.
- A second completion command is rejected without creating duplicate movements.
- A persistence failure rolls back material consumption, finished-product receipt, reservation state, and production status together.

**Postconditions:** Material and finished-product movements explain the completed production, and no further completion command is valid.

### UC-11: Cancel Production and Release a Reservation

**Primary actor:** Planner or Inventory manager  

**Main success scenario:**

1. The actor requests cancellation with a reason.
2. The system verifies that the production order has not completed or consumed materials.
3. The system releases any active reservation.
4. The system records the release movement and cancellation actor/time/reason.
5. The system marks the production order cancelled.

**Alternative and exception flows:**

- A completed production order cannot be cancelled through this MVP command.
- An in-progress order cannot be cancelled when doing so would misrepresent physically consumed material.
- A duplicate cancellation request is rejected or returns the already-cancelled result according to the API contract.

**Postconditions:** The order is closed, the reservation is no longer available to it, and on-hand quantity has not changed.

### UC-12: Review Workflow and Inventory Traceability

**Primary actor:** Any authorized internal user  

**Main success scenario:**

1. The user opens a customer order, production order, inventory balance, or movement history.
2. The system returns current status, key timestamps, references, and related records permitted to that role.
3. The user can trace a completed production order to its BOM snapshot, material movements, quality result, and finished-product receipt.

**Alternative and exception flows:**

- Unauthorized related records are not exposed through the response.
- A missing identifier returns a consistent not-found error.
- Large movement collections use the documented pagination or filtering behaviour.

**Postconditions:** The user can understand the operational state without direct database access.

## 4. End-to-End Scenario

The primary demonstration should execute the following use cases in order:

```text
UC-01 Authenticate
  -> UC-02 Maintain catalog
  -> UC-03 Activate BOM
  -> UC-04 Record inventory
  -> UC-05 Confirm order
  -> UC-06 Plan production
  -> UC-07 Reserve materials
  -> UC-08 Execute stages
  -> UC-09 Pass quality inspection
  -> UC-10 Complete production
  -> UC-12 Review traceability
```

The most important rejection paths are:

- UC-06 without an active BOM
- UC-07 with insufficient material
- UC-07 competing with another reservation
- UC-08 attempting to skip a stage
- UC-09 failing quality
- UC-10 attempting completion without a passing inspection
- UC-10 failing partway through persistence

## 5. Acceptance Scenarios

### Scenario A: BOM changes do not rewrite planned production

```text
Given product P has active BOM revision 1
And a planner creates production order PO from product P
When BOM revision 2 becomes active for product P
Then PO continues to use revision 1 and its copied material requirements
```

### Scenario B: Insufficient stock is atomic

```text
Given PO requires material A and material B
And material A is available but material B is insufficient
When the planner requests reservation
Then the reservation fails
And neither A nor B has increased reserved quantity
And PO remains planned
```

### Scenario C: Concurrent reservations do not oversell

```text
Given two production orders each require more material than can be shared
When both reservation commands execute concurrently
Then at most one command succeeds
And available stock never becomes negative
And reserved quantity never exceeds on-hand quantity
```

### Scenario D: Quality blocks completion

```text
Given all production stages are complete
When a quality inspector records a failed inspection
Then the production order cannot be completed
And the defects remain queryable
And no completion inventory movements are created
```

### Scenario E: Completion is all-or-nothing

```text
Given a production order has passed quality and has an active reservation
When a failure occurs while applying the completion transaction
Then material consumption, finished-product receipt, reservation state, and production status are all rolled back
```

## 6. Use-Case Design Decisions

| Decision | Why it is appropriate | Alternative rejected | Interview and reengineering benefit |
| --- | --- | --- | --- |
| Describe business actions rather than individual screens | The same use case can be exercised by REST, the React client, or tests | Define the system as a list of UI pages | Keeps domain behaviour independent from presentation and supports future clients |
| Make one complete order-to-completion scenario the primary demonstration | It proves that module boundaries work together | Implement unrelated CRUD features first | Produces a clear portfolio narrative and reveals integration defects early |
| Treat the customer as a business record in the MVP | Authentication and manufacturing roles remain focused on internal operations | Build a customer portal and external login | Reduces security and support scope while leaving a future extension path |
| Treat reservation, stage transitions, inspection, and completion as commands | These actions change state and require validation and transactions | Expose generic update operations for status fields | Makes invariants harder to bypass and gives the API a meaningful domain vocabulary |
| Keep failure paths in the primary use-case documentation | Manufacturing systems are judged by safe rejection and rollback, not just happy paths | Document only successful workflows | Demonstrates reliability thinking and guides automated test coverage |
