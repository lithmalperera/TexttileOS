# API Specification

**Project:** Textile Manufacturing Management System
**Document status:** Reduced MVP contract outline
**Implementation status:** Identity user endpoints are being implemented; remaining business endpoints are planned

## 1. Base Contract

- Base path: `/api/v1`
- Format: JSON
- Authentication: `Authorization: Bearer <JWT>` for business endpoints
- IDs: UUID strings
- Times: ISO-8601 UTC
- Decimal quantities: JSON strings such as `"12.500"`
- Product quantities: positive integers
- Errors: Problem Details-inspired response with stable application code

## 2. Status Codes

- `200`: successful read or command with a response
- `201`: resource created
- `204`: successful command with no body
- `400`: malformed request or boundary validation
- `401`: missing or invalid authentication
- `403`: authenticated but not authorized
- `404`: resource not found
- `409`: duplicate, stale, concurrent, or invalid current-state conflict
- `422`: valid request that violates a domain rule
- `500`: unexpected safe server error

## 3. Authentication and Users

- `POST /api/v1/auth/login`: authenticate and issue JWT
- `GET /api/v1/auth/me`: current user and roles
- `GET /api/v1/users`: administrator user list
- `POST /api/v1/users`: administrator creates an internal user
- `GET /api/v1/users/{userId}`: administrator reads a user
- `POST /api/v1/users/{userId}/deactivation`: administrator deactivates a user

There is no customer login, refresh-token endpoint, password-reset endpoint, or public registration in the MVP.

## 4. Catalog

Products:

- `GET /api/v1/products`
- `POST /api/v1/products`
- `GET /api/v1/products/{productId}`
- `PATCH /api/v1/products/{productId}`
- `POST /api/v1/products/{productId}/archival`

Materials:

- `GET /api/v1/materials`
- `POST /api/v1/materials`
- `GET /api/v1/materials/{materialId}`
- `PATCH /api/v1/materials/{materialId}`
- `POST /api/v1/materials/{materialId}/archival`

Simple BOM:

- `GET /api/v1/products/{productId}/bom`
- `PUT /api/v1/products/{productId}/bom`
- `POST /api/v1/bom-calculations`

The MVP has no BOM revision activation endpoint. A later revision workflow is `EXT-002`.

## 5. Inventory

- `GET /api/v1/inventory/balances`
- `GET /api/v1/inventory/balances/{inventoryItemId}`
- `GET /api/v1/inventory/movements`
- `POST /api/v1/inventory/receipts`
- `POST /api/v1/inventory/adjustments`

Reservation and consumption are commands on production orders so Inventory remains the only owner of stock mutation.

## 6. Manufacturing

Manufacturing orders:

- `GET /api/v1/manufacturing-orders`
- `POST /api/v1/manufacturing-orders`
- `GET /api/v1/manufacturing-orders/{orderId}`
- `POST /api/v1/manufacturing-orders/{orderId}/production-order`

Production:

- `GET /api/v1/production-orders`
- `GET /api/v1/production-orders/{productionOrderId}`
- `GET /api/v1/production-orders/{productionOrderId}/requirements`
- `POST /api/v1/production-orders/{productionOrderId}/material-reservation`
- `POST /api/v1/production-orders/{productionOrderId}/cancellation`
- `POST /api/v1/production-orders/{productionOrderId}/stages/start`
- `POST /api/v1/production-orders/{productionOrderId}/stages/completion`
- `GET /api/v1/production-orders/{productionOrderId}/quality-inspection`
- `POST /api/v1/production-orders/{productionOrderId}/quality-inspection`
- `POST /api/v1/production-orders/{productionOrderId}/completion`

There is no separate customer, order-line, stage, or defect endpoint in the MVP.

## 7. Error Shape

```json
{
  "type": "https://example.invalid/problems/insufficient-material",
  "title": "Material reservation failed",
  "status": 422,
  "code": "INSUFFICIENT_MATERIAL",
  "message": "One or more required materials are not available.",
  "path": "/api/v1/production-orders/123/material-reservation",
  "timestamp": "2026-09-23T12:30:00Z",
  "traceId": "a2f9f3c6e9fd4cd2",
  "fieldErrors": []
}
```

Unexpected errors return a generic message. Stack traces, SQL, password hashes, and tokens never appear in responses.

## 8. Pagination

List endpoints use bounded page parameters:

```text
?page=0&size=20&sort=createdAt,desc
```

The response uses the project-owned `PageResponse` DTO rather than exposing Spring's internal `PageImpl` JSON shape.

## 9. OpenAPI

The generated specification must include:

- JWT bearer scheme
- DTO fields and validation constraints
- status values and units
- success and error responses
- pagination parameters
- examples for reservation, quality, and completion commands

Swagger UI is the primary way to exercise setup and API behaviour before the React client is complete.

## 10. API Decisions

### Explicit commands instead of generic status PATCH

- **Decision:** use command subresources such as `/completion` and `/material-reservation`.
- **Why:** each command has a transaction and invariant boundary.
- **Rejected:** let clients set arbitrary `status` fields.
- **Benefit:** clients cannot bypass inventory or quality rules.

### One manufacturing order resource

- **Decision:** use one resource containing customer reference, product, and quantity.
- **Why:** enough demand traceability for the MVP.
- **Rejected:** separate customer, order, and order-line APIs.
- **Benefit:** a smaller API with the same end-to-end narrative.
