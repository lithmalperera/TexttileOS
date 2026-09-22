# Approved Feature Backlog

These are approved MVP implementation items. They are ordered roughly by dependency, not by the number of files they will require.

**Status values:** `PLANNED`, `IN_PROGRESS`, `REVIEW`, `COMPLETED`, `DEFERRED`, `REJECTED`

## Foundation

### FND-001: Git Repository and Project Hygiene

- **Owner module:** Project
- **Priority:** P0
- **Depends on:** None
- **Acceptance:** The repository has a safe `.gitignore`, documented commands, and no secrets or generated files committed.
- **Status:** `COMPLETED`

### FND-002: Spring Boot Backend Bootstrap

- **Owner module:** Project
- **Priority:** P0
- **Depends on:** FND-001
- **Acceptance:** The backend builds, starts, and exposes the planned package structure without business behaviour.
- **Status:** `PLANNED`

### FND-003: React and Vite Frontend Bootstrap

- **Owner module:** Project
- **Priority:** P0
- **Depends on:** FND-001
- **Acceptance:** The frontend installs, starts, type-checks, and renders a minimal shell.
- **Status:** `PLANNED`

### FND-004: Local PostgreSQL with Docker Compose

- **Owner module:** Project
- **Priority:** P0
- **Depends on:** FND-001, FND-002
- **Acceptance:** PostgreSQL starts from documented configuration and the backend can connect to it.
- **Status:** `PLANNED`

### FND-005: Migration, Health, Error, and OpenAPI Foundation

- **Owner module:** Project
- **Priority:** P0
- **Depends on:** FND-002, FND-004
- **Acceptance:** Migrations run on a clean database, health responds, errors have one shape, and OpenAPI is available.
- **Status:** `PLANNED`

## Identity and Access

### IAM-001: Internal Users and Fixed Roles

- **Owner module:** Identity and Access
- **Priority:** P0
- **Depends on:** FND-005
- **Acceptance:** An administrator can create and deactivate users and assign the five documented roles.
- **Status:** `PLANNED`

### IAM-002: JWT Login and Server-Side Authorization

- **Owner module:** Identity and Access
- **Priority:** P0
- **Depends on:** IAM-001
- **Acceptance:** Valid users receive short-lived JWTs and protected commands enforce role permissions.
- **Status:** `PLANNED`

## Catalog and BOM

### CAT-001: Product and Material Catalog

- **Owner module:** Products, Materials
- **Priority:** P0
- **Depends on:** IAM-002
- **Acceptance:** Authorized users can create, validate, view, update, and archive master records.
- **Status:** `PLANNED`

### BOM-001: BOM Revisions and Requirement Calculation

- **Owner module:** BOM
- **Priority:** P0
- **Depends on:** CAT-001
- **Acceptance:** Draft BOMs can be activated, only one revision is active, and requirements calculate correctly.
- **Status:** `PLANNED`

## Inventory

### INV-001: Stock Balances, Receipts, Adjustments, and Ledger

- **Owner module:** Inventory
- **Priority:** P0
- **Depends on:** CAT-001, FND-005
- **Acceptance:** Stock balances and append-only movements remain consistent after receipts and adjustments.
- **Status:** `PLANNED`

### INV-002: Atomic Material Reservations and Release

- **Owner module:** Inventory, Production
- **Priority:** P0
- **Depends on:** BOM-001, INV-001
- **Acceptance:** Required materials reserve all-or-nothing, release safely, and cannot be oversold concurrently.
- **Status:** `PLANNED`

## Customers and Orders

### CUS-001: Customers and Customer Orders

- **Owner module:** Customers, Customer Orders
- **Priority:** P0
- **Depends on:** CAT-001, IAM-002
- **Acceptance:** A planner can create and confirm valid orders with stable order lines.
- **Status:** `PLANNED`

## Production

### PROD-001: Production Order and BOM Snapshot

- **Owner module:** Production
- **Priority:** P0
- **Depends on:** BOM-001, CUS-001
- **Acceptance:** One confirmed order line creates one full-quantity production order with immutable requirements.
- **Status:** `PLANNED`

### STG-001: Fixed Production-Stage Execution

- **Owner module:** Production Stages
- **Priority:** P0
- **Depends on:** PROD-001, INV-002
- **Acceptance:** Operators can progress through the fixed route without skipping or repeating stages.
- **Status:** `PLANNED`

### QLT-001: Final Quality Inspection and Defects

- **Owner module:** Quality and Defects
- **Priority:** P0
- **Depends on:** STG-001
- **Acceptance:** A quality inspector can record pass/fail and defects; failure blocks completion.
- **Status:** `PLANNED`

### FLOW-001: Transactional Production Completion

- **Owner module:** Workflow, Production, Inventory
- **Priority:** P0
- **Depends on:** INV-002, QLT-001
- **Acceptance:** Passing production consumes reserved materials, receives finished stock, and closes statuses atomically.
- **Status:** `PLANNED`

## Frontend and Quality

### UI-001: Thin Operational Workflow Client

- **Owner module:** Frontend
- **Priority:** P1
- **Depends on:** IAM-002, FLOW-001
- **Acceptance:** A reviewer can exercise login and the main order-to-completion flow without database edits.
- **Status:** `PLANNED`

### QA-001: MVP Hardening and Release Verification

- **Owner module:** Project
- **Priority:** P0
- **Depends on:** All P0 items
- **Acceptance:** Unit, PostgreSQL integration, concurrency, security, API, and workflow tests pass from a clean setup.
- **Status:** `PLANNED`

## Backlog Rules

- A P0 item supports the documented MVP and should be completed before adding optional features.
- A feature cannot be marked `COMPLETED` because code exists alone; its acceptance criteria, tests, security checks, and documentation must also be complete.
- If scope changes, update the relevant requirements or business-rules document before changing the backlog item.
- Move only the current item or small active slice to `in-progress.md` to keep work visible and manageable.
