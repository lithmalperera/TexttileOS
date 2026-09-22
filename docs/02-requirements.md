# Requirements

**Project:** Textile Manufacturing Management System  
**Document status:** Proposed  
**Implementation status:** Documentation only

This document converts the project overview into a set of testable MVP requirements. It intentionally describes business capabilities and observable behaviour, not classes, database tables, or endpoint paths. Those implementation details belong in the architecture, database, and API documents.

## 1. Scope and Requirement Notation

The requirements apply to the focused small-batch cut-and-sew workflow described in [`01-project-overview.md`](01-project-overview.md).

Each requirement has an identifier so that use cases, business rules, tests, and API documentation can refer back to a stable statement.

- **MUST:** Required for the MVP.
- **SHOULD:** Valuable for the MVP if it does not delay the complete core workflow.
- **LATER:** Explicitly deferred; not a delivery commitment for the MVP.

An MVP requirement is complete only when its success path, important rejection paths, authorization, persistence behaviour, and documentation are addressed.

## 2. Product Objective

The system must allow an authenticated internal user to manage the definitions and records needed to turn a confirmed customer-order line into a quality-approved production result while keeping inventory consistent and auditable.

The project is successful when a reviewer can run the application, authenticate, create the relevant master data, record stock, execute the core workflow, inspect the resulting stock movements, and understand the design from the documentation and OpenAPI contract.

## 3. Actors and Responsibilities

| Actor | Requirements responsibility |
| --- | --- |
| Administrator | Manage internal users, roles, and system-level master data |
| Planner | Manage product definitions, materials, BOM revisions, customer orders, and production planning |
| Inventory manager | Record stock receipts and adjustments, inspect availability, and perform inventory commands |
| Production operator | Start and advance production stages |
| Quality inspector | Record inspections and defects and decide pass/fail outcome |
| Customer record | Provide business information for a customer order; has no login in the MVP |

The backend must enforce permissions. Hiding a button in the frontend is not an authorization control.

## 4. Functional Requirements

### 4.1 Identity and Access

| ID | Priority | Requirement |
| --- | --- | --- |
| FR-IAM-01 | MUST | An administrator can create an internal user with a unique login identifier, display name, and assigned role. |
| FR-IAM-02 | MUST | A user can authenticate with valid credentials and receive a JWT suitable for calling protected API operations. |
| FR-IAM-03 | MUST | Protected operations reject requests with no token, an invalid token, an expired token, or a token that does not identify an active account. |
| FR-IAM-04 | MUST | The backend enforces role-based authorization for administrative, planning, inventory, production, and quality operations. |
| FR-IAM-05 | MUST | An administrator can deactivate a user account. A deactivated account cannot start a new authenticated session, subject to the documented token policy. |
| FR-IAM-06 | MUST | Authentication failures do not reveal whether a login identifier exists or disclose sensitive credential details. |
| FR-IAM-07 | SHOULD | The authenticated-user response identifies the current user and effective roles so the frontend can display an appropriate workflow. |

### 4.2 Products

| ID | Priority | Requirement |
| --- | --- | --- |
| FR-PRD-01 | MUST | An authorized planner or administrator can create a product with a unique code/SKU, name, category, description, and output unit. |
| FR-PRD-02 | MUST | An authorized user can view, update permitted fields, and archive a product. |
| FR-PRD-03 | MUST | Product codes are unique under the documented case and whitespace rules. |
| FR-PRD-04 | MUST | Archived products cannot be used in new customer orders, BOM activation, or production planning. |
| FR-PRD-05 | MUST | Existing records that reference an archived product remain readable and historically valid. |

### 4.3 Materials

| ID | Priority | Requirement |
| --- | --- | --- |
| FR-MAT-01 | MUST | An authorized planner or administrator can create a material with a unique code, name, material type, and base unit. |
| FR-MAT-02 | MUST | An authorized user can view, update permitted fields, and archive a material. |
| FR-MAT-03 | MUST | Material codes are unique under the documented case and whitespace rules. |
| FR-MAT-04 | MUST | Archived materials cannot be added to new BOM revisions or used for new stock receipts, while existing historical references remain readable. |
| FR-MAT-05 | MUST | Material quantities use decimal-safe values and are never represented as binary floating-point values in business calculations. |

### 4.4 Bill of Materials

| ID | Priority | Requirement |
| --- | --- | --- |
| FR-BOM-01 | MUST | An authorized planner can create a draft, single-level BOM revision for an active product. |
| FR-BOM-02 | MUST | A BOM revision contains one or more material lines with positive quantity-per-product-unit values. |
| FR-BOM-03 | MUST | A BOM revision rejects duplicate material lines and inactive or incompatible material references. |
| FR-BOM-04 | MUST | At most one BOM revision for a product is active at a time. Activating a new revision retires the previous active revision without rewriting its history. |
| FR-BOM-05 | MUST | A draft BOM can be edited before activation; an active or retired revision is immutable. |
| FR-BOM-06 | MUST | The system can calculate required material quantities for a requested product quantity from an active BOM. |
| FR-BOM-07 | MUST | When a production order is created, the active BOM revision and its lines are copied into a production-specific snapshot. |

### 4.5 Inventory

| ID | Priority | Requirement |
| --- | --- | --- |
| FR-INV-01 | MUST | The system tracks on-hand and reserved quantities for raw materials and finished products within one logical stock location. |
| FR-INV-02 | MUST | An authorized inventory manager can record a stock receipt with item, quantity, reason or reference, actor, and timestamp. |
| FR-INV-03 | MUST | An authorized inventory manager can record a controlled stock adjustment with a mandatory reason. |
| FR-INV-04 | MUST | Every stock-changing operation creates an append-only inventory movement record. |
| FR-INV-05 | MUST | The system exposes available quantity as on-hand quantity minus reserved quantity. |
| FR-INV-06 | MUST | The system can reserve all material requirements for a production order only when every required material has sufficient available quantity. |
| FR-INV-07 | MUST | A failed multi-line reservation changes neither the stock balances nor the reservation records for that command. |
| FR-INV-08 | MUST | A valid cancellation before material consumption releases the production order's active reservation without changing on-hand quantity. |
| FR-INV-09 | MUST | Successful production completion consumes the reserved material quantity and records the finished-product receipt. |
| FR-INV-10 | MUST | Competing reservation requests cannot both claim the same available quantity. |
| FR-INV-11 | SHOULD | Inventory queries can filter movements by item, movement type, date range, and business reference. |

### 4.6 Customers

| ID | Priority | Requirement |
| --- | --- | --- |
| FR-CUS-01 | MUST | An authorized planner or administrator can create and maintain a customer record with identifying and contact information. |
| FR-CUS-02 | MUST | Customer records have a stable identifier and remain readable when referenced by historical orders. |
| FR-CUS-03 | MUST | A customer can be marked inactive without deleting historical orders. |

### 4.7 Customer Orders

| ID | Priority | Requirement |
| --- | --- | --- |
| FR-ORD-01 | MUST | An authorized planner can create a draft customer order for an active customer. |
| FR-ORD-02 | MUST | An order contains one or more lines with an active product and a positive whole-unit quantity. |
| FR-ORD-03 | MUST | A draft order can be corrected before confirmation. |
| FR-ORD-04 | MUST | Confirmation validates the customer, product status, quantities, and required order dates before changing the order state. |
| FR-ORD-05 | MUST | A confirmed order line can be selected for one production order in the MVP; partial line splitting is not supported. |
| FR-ORD-06 | MUST | An order cannot be cancelled after a production order has been created for one of its lines. |
| FR-ORD-07 | MUST | The order lifecycle and line-to-production relationship are visible through the API. |

### 4.8 Production Orders

| ID | Priority | Requirement |
| --- | --- | --- |
| FR-PROD-01 | MUST | An authorized planner can create a production order for the full quantity of one confirmed customer-order line. |
| FR-PROD-02 | MUST | Production creation fails when the selected product has no active BOM. |
| FR-PROD-03 | MUST | Production creation stores the BOM revision and material-requirement snapshot used for that order. |
| FR-PROD-04 | MUST | A production order exposes an explicit lifecycle and rejects invalid state transitions. |
| FR-PROD-05 | MUST | A production order can request material reservation only after its requirements have been calculated. |
| FR-PROD-06 | MUST | A production order cannot be completed more than once. |
| FR-PROD-07 | MUST | A production order can be cancelled only in states where the documented reservation and consumption consequences are safe. |

### 4.9 Production Stages

| ID | Priority | Requirement |
| --- | --- | --- |
| FR-STG-01 | MUST | Each production order receives the fixed, ordered MVP route: preparation/cutting, sewing or assembly, and finishing. |
| FR-STG-02 | MUST | An authorized production operator can start and complete only the current stage. |
| FR-STG-03 | MUST | The system rejects skipped, repeated, or out-of-order stage transitions. |
| FR-STG-04 | MUST | Stage execution records include status and relevant timestamps. |
| FR-STG-05 | MUST | Completing the final production stage moves the order to the quality-pending state. |

### 4.10 Quality and Defects

| ID | Priority | Requirement |
| --- | --- | --- |
| FR-QLT-01 | MUST | An authorized quality inspector can create a final inspection for a production order awaiting quality review. |
| FR-QLT-02 | MUST | An inspection can record outcome, defect category, affected quantity, severity or disposition, notes, actor, and timestamp. |
| FR-QLT-03 | MUST | A failed inspection records the defects and prevents successful production completion. |
| FR-QLT-04 | MUST | A passing inspection allows production completion only when all other completion preconditions are satisfied. |
| FR-QLT-05 | SHOULD | Quality queries can filter inspections by production order, outcome, date, and defect category. |

### 4.11 API, Documentation, and Frontend

| ID | Priority | Requirement |
| --- | --- | --- |
| FR-API-01 | MUST | REST endpoints use request and response DTOs and do not expose persistence entities directly. |
| FR-API-02 | MUST | Invalid input produces a consistent error response with enough detail for a client to correct the request without exposing internals. |
| FR-API-03 | MUST | Implemented REST endpoints are described in OpenAPI/Swagger, including authentication, validation errors, and relevant response codes. |
| FR-API-04 | SHOULD | Collection endpoints support predictable pagination or bounded result sizes where unbounded growth is possible. |
| FR-UI-01 | MUST | The React client provides enough screens to demonstrate authentication and the main order-to-completion workflow. |
| FR-UI-02 | MUST | The frontend handles loading, empty, validation, authentication, authorization, and expected business-error states. |
| FR-UI-03 | MUST | The frontend does not reimplement material calculations, authorization rules, or inventory mutation logic. |

## 5. Non-Functional Requirements

| ID | Priority | Requirement |
| --- | --- | --- |
| NFR-SEC-01 | MUST | Passwords are stored only as strong password hashes; plaintext passwords are never persisted or logged. |
| NFR-SEC-02 | MUST | Authorization is enforced on the server for every protected business operation. |
| NFR-SEC-03 | MUST | Secrets, signing keys, and database credentials are supplied through configuration and are not committed to source control. |
| NFR-DATA-01 | MUST | PostgreSQL constraints and service validation protect required relationships, uniqueness, and valid quantity/state ranges. |
| NFR-CON-01 | MUST | Reservation, release, consumption, and finished-stock receipt have explicit transaction boundaries. |
| NFR-CON-02 | MUST | Concurrent inventory commands cannot leave a balance with reserved quantity greater than on-hand quantity. |
| NFR-CON-03 | MUST | A failure in a multi-record business operation rolls back all changes that belong to that operation. |
| NFR-TEST-01 | MUST | Core business rules have unit tests, and database-sensitive workflows have PostgreSQL-backed integration tests. |
| NFR-TEST-02 | MUST | At least one automated test exercises competing material reservations. |
| NFR-OPS-01 | MUST | The application can be run locally with documented Docker Compose and environment configuration. |
| NFR-OPS-02 | SHOULD | Logs include useful correlation or business references without logging passwords, tokens, or unnecessary personal data. |
| NFR-API-01 | MUST | API status codes and error responses are consistent across modules. |
| NFR-DOC-01 | MUST | Requirements, business rules, API behaviour, security assumptions, and test strategy stay aligned with the implementation. |
| NFR-MAINT-01 | MUST | Module ownership is visible in the code structure and cross-module repository access is prohibited by convention and review. |
| NFR-PERF-01 | MUST | Normal portfolio/demo workloads respond predictably without avoidably loading unbounded collections or performing repeated database lookups. No production-scale SLA is claimed for the MVP. |
| NFR-UX-01 | SHOULD | The main workflow is understandable to a reviewer without requiring direct database edits or knowledge of internal implementation details. |

## 6. MVP Acceptance Criteria

The MVP requirements are collectively satisfied when all of the following scenarios work:

1. A valid administrator can create a planner, inventory manager, production operator, and quality inspector account.
2. A planner can create an active product, active materials, and one active BOM revision.
3. An inventory manager can record raw-material stock and inspect the resulting balance and movement history.
4. A planner can create and confirm a customer order.
5. A planner can create one production order for a confirmed order line and view its BOM snapshot and requirements.
6. Inventory can reserve the requirements when enough stock is available and reject the request atomically when any line is short.
7. Two competing reservations cannot both succeed against the same limited stock.
8. A production operator can progress through the fixed stages, but cannot skip or repeat a stage.
9. A quality inspector can record a passing or failing inspection with defects.
10. A failed inspection blocks completion; a passing inspection permits it when other preconditions are satisfied.
11. Successful completion creates the expected material-consumption and finished-product movements in one transaction.
12. An unauthorized user cannot perform an operation outside the assigned role, even if the HTTP request is sent directly.
13. The workflow and its expected failures are visible through OpenAPI, automated tests, and the documentation.

## 7. Explicitly Deferred Requirements

The following are not MVP requirements:

- Supplier management and purchase orders
- Accounting, invoicing, payments, and full product costing
- Multi-warehouse stock, transfers, lots, serials, expiry, and barcode operations
- Multi-level or alternative BOMs and automated waste/yield calculations
- MRP, demand forecasting, finite-capacity scheduling, and optimization
- Machine telemetry, maintenance, and external manufacturing integrations
- Automated rework, corrective-action plans, and statistical process control
- Customer portal, customer login, and external identity providers
- GraphQL, microservices, Kubernetes, Redis, RabbitMQ, and service discovery
- Multi-tenancy and a full ERP reporting suite

## 8. Requirements Decisions

| Decision | Why it is appropriate | Alternative rejected | Interview and reengineering benefit |
| --- | --- | --- | --- |
| Use stable requirement IDs and acceptance scenarios | Business rules, tests, and API operations can trace back to an explicit expectation | Track scope only through informal task titles | Demonstrates requirements traceability and makes regressions easier to diagnose |
| Make the MVP production quantity equal to the full selected order-line quantity | Avoids partial-allocation, split-production, and remaining-quantity rules in the first release | Support partial production and multiple production orders per line immediately | Shows deliberate scope control while preserving a clear future extension point |
| Make quality pass a completion prerequisite | Prevents inventory from presenting failed output as finished goods | Allow operators to complete first and repair status later | Demonstrates a domain invariant rather than treating quality as a passive note |
| Require all-or-nothing material reservations | Prevents production orders from entering a misleading partially reserved state | Reserve whatever is available and resolve shortages later | Demonstrates atomic business operations and predictable failure handling |
| Use qualitative MVP performance expectations instead of invented scale targets | A portfolio project should avoid claiming production capacity it has not measured | Add arbitrary throughput or uptime promises | Shows honest engineering judgement and leaves room for evidence-based tuning |
