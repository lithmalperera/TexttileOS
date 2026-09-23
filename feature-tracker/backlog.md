# Approved Feature Backlog

This is the reduced MVP backlog. It prioritizes one complete manufacturing workflow and strong backend behaviour over broad ERP coverage.

**Status values:** `PLANNED`, `IN_PROGRESS`, `REVIEW`, `COMPLETED`, `DEFERRED`, `REJECTED`

## Foundation

### FND-001: Git Repository and Project Hygiene

- **Owner:** Project
- **Priority:** P0
- **Status:** `COMPLETED`

### FND-002: Spring Boot Backend Bootstrap

- **Owner:** Project
- **Priority:** P0
- **Status:** `COMPLETED`

### FND-003: React and Vite Frontend Bootstrap

- **Owner:** Project
- **Priority:** P0
- **Status:** `COMPLETED`

### FND-004: Local PostgreSQL with Docker Compose

- **Owner:** Project
- **Priority:** P0
- **Status:** `COMPLETED`

### FND-005: Migration, Health, Error, and OpenAPI Foundation

- **Owner:** Project
- **Priority:** P0
- **Status:** `COMPLETED`

## Identity and Access

### IAM-001: Internal Users and Fixed Roles

- **Owner:** Identity and Access
- **Priority:** P0
- **Depends on:** FND-005
- **Acceptance:** Admin user management works with BCrypt, fixed roles, DTOs, validation, safe errors, and tests.
- **Status:** `COMPLETED`

### IAM-002: JWT Login and Server-Side Authorization

- **Owner:** Identity and Access
- **Priority:** P0
- **Depends on:** IAM-001
- **Acceptance:** Valid users receive short-lived JWTs; active roles control protected operations.
- **Status:** `PLANNED`

## Catalog

### CAT-001: Products and Materials

- **Owner:** Catalog
- **Priority:** P0
- **Depends on:** IAM-002
- **Acceptance:** Authorized users can create, validate, inspect, update, and archive products and materials.
- **Status:** `PLANNED`

### BOM-001: Simple Active BOM and Snapshot Calculation

- **Owner:** Catalog
- **Priority:** P0
- **Depends on:** CAT-001
- **Acceptance:** One active single-level BOM calculates requirements and is copied into production requirements.
- **Status:** `PLANNED`

## Inventory

### INV-001: Stock Balances and Movement Ledger

- **Owner:** Inventory
- **Priority:** P0
- **Depends on:** CAT-001, FND-005
- **Acceptance:** Receipts and adjustments update consistent balances and append movements.
- **Status:** `PLANNED`

### INV-002: Atomic Reservations and Release

- **Owner:** Inventory
- **Priority:** P0
- **Depends on:** BOM-001, INV-001
- **Acceptance:** Reservations are all-or-nothing, row-locked, auditable, and safe under concurrent requests.
- **Status:** `PLANNED`

## Manufacturing

### MFG-001: Manufacturing Order, Production, and Fixed Stages

- **Owner:** Manufacturing
- **Priority:** P0
- **Depends on:** BOM-001, INV-002
- **Acceptance:** One-product manufacturing demand creates production with a BOM snapshot and fixed stage transitions.
- **Status:** `PLANNED`

### MFG-002: Quality Gate and Transactional Completion

- **Owner:** Manufacturing, Inventory
- **Priority:** P0
- **Depends on:** MFG-001, INV-002
- **Acceptance:** Quality pass enables one atomic completion; failure blocks it; stock movements and statuses remain consistent.
- **Status:** `PLANNED`

## Frontend and Release

### UI-001: Thin Manufacturing Workflow Client

- **Owner:** Frontend
- **Priority:** P1
- **Depends on:** IAM-002, MFG-002
- **Acceptance:** A reviewer can log in and exercise the main workflow without direct database edits.
- **Status:** `PLANNED`

### QA-001: MVP Hardening and Portfolio Release

- **Owner:** Project
- **Priority:** P0
- **Depends on:** All active P0 items
- **Acceptance:** Key service, PostgreSQL, security, concurrency, rollback, API, and workflow tests pass from a clean setup.
- **Status:** `PLANNED`

## Deferred Extensions

These are not active backlog work, but the MVP leaves explicit extension points:

### EXT-001: Customer Master and Multi-Line Orders

Future `customer`, `customer_order`, and `customer_order_line` tables.

### EXT-002: BOM Revisions and Engineering Change Control

Future draft/active/retired revisions and effective dates.

### EXT-003: Separate Production Stage Module

Future stage definitions, execution history, pause/resume, and configurable routes.

### EXT-004: Detailed Quality and Rework

Future defect records, dispositions, rework, and repeat inspections.

### EXT-005: Advanced Inventory and Procurement

Future warehouses, transfers, lots, serials, suppliers, purchasing, and costing.

## Backlog Rules

- P0 active items support the reduced MVP.
- Do not move deferred extensions into the active backlog without updating the design documents.
- A feature is complete only after code, tests, security review, documentation, and developer verification.
- Keep only the current small slice in `in-progress.md`.
