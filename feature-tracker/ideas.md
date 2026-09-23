# Feature Ideas

These ideas are suggestions, not approved scope. Some were generated from common manufacturing-system requirements or AI-assisted brainstorming. Each one needs a human decision before entering the backlog.

## IDEA-001: Low-Stock Indicators and Reorder Thresholds

- **User problem:** Inventory users need to notice material shortages before planning production.
- **Portfolio value:** Adds useful read-side logic and a practical operational screen.
- **Complexity:** Low
- **Recommendation:** Consider after the core reservation flow.
- **Decision:** AI suggestion; review later.

## IDEA-002: Inventory Activity and Audit Timeline

- **User problem:** Reviewers need to understand who changed a balance and why.
- **Portfolio value:** Makes the movement ledger visible and demonstrates traceability.
- **Complexity:** Low
- **Recommendation:** Add after inventory is stable.
- **Decision:** AI suggestion; likely useful.

## IDEA-003: CSV Import and Export for Products and Materials

- **User problem:** Initial master-data setup can be repetitive.
- **Portfolio value:** Demonstrates validation and controlled bulk operations.
- **Complexity:** Medium
- **Recommendation:** Defer until manual setup is reliable.
- **Decision:** AI suggestion; defer.

## IDEA-004: Batch or Lot Traceability

- **User problem:** Manufacturers may need to trace material lots into finished goods.
- **Portfolio value:** Adds realistic traceability and stronger relational modelling.
- **Complexity:** High
- **Recommendation:** Keep outside the MVP.
- **Decision:** AI suggestion; defer.

## IDEA-005: Rework and Repeat Quality Inspection

- **User problem:** Failed output may need correction instead of cancellation.
- **Portfolio value:** Adds a realistic quality lifecycle and state-machine depth.
- **Complexity:** Medium
- **Recommendation:** Consider as the first post-MVP workflow.
- **Decision:** AI suggestion; defer.

## IDEA-006: Supplier and Purchase-Order Workflow

- **User problem:** Stock usually arrives from a supplier.
- **Portfolio value:** Expands the system toward procurement and inbound lifecycle modelling.
- **Complexity:** High
- **Recommendation:** Do not add to the MVP.
- **Decision:** AI suggestion; reject for current scope.

## IDEA-007: Multiple Warehouses and Stock Transfers

- **User problem:** Larger manufacturers move material between locations.
- **Portfolio value:** Demonstrates allocation, transfer, and location-level locking.
- **Complexity:** High
- **Recommendation:** Do not add until one-location inventory is proven.
- **Decision:** AI suggestion; defer.

## IDEA-008: Production Capacity and Scheduling

- **User problem:** Planners need to know whether stages can meet a due date.
- **Portfolio value:** Demonstrates planning algorithms and resource constraints.
- **Complexity:** High
- **Recommendation:** Keep outside the MVP.
- **Decision:** AI suggestion; reject for current scope.

## IDEA-009: API Idempotency Keys for External Retries

- **User problem:** Network retries can duplicate receipt or command requests.
- **Portfolio value:** Demonstrates reliable integration semantics.
- **Complexity:** Medium
- **Recommendation:** Add only when an external integration exists.
- **Decision:** AI suggestion; defer.

## IDEA-010: Simple Production Dashboard

- **User problem:** Managers need a quick view of planned, active, blocked, and completed work.
- **Portfolio value:** Improves demonstration value without changing core domain rules.
- **Complexity:** Low
- **Recommendation:** Add after the thin workflow UI.
- **Decision:** AI suggestion; consider.

## IDEA-011: Notifications for Quality Failure or Shortage

- **User problem:** Users may miss blocked production orders.
- **Portfolio value:** Demonstrates asynchronous delivery if a real need exists.
- **Complexity:** Medium
- **Recommendation:** Start with in-app status; defer messaging.
- **Decision:** AI suggestion; defer.

## Idea Evaluation Rules

- An idea that adds a new lifecycle must first receive a use case and business-rule design.
- An idea that changes inventory must include concurrency, audit, and rollback analysis.
- An idea that adds infrastructure must explain why an in-process or PostgreSQL solution is insufficient.
- An idea that cannot be demonstrated in a small, testable workflow should not enter the MVP backlog.
- A feature is not valuable merely because it sounds enterprise-like; it must solve a defined problem or demonstrate a meaningful engineering skill.
