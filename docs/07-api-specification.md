# API Specification

**Project:** Textile Manufacturing Management System  
**Document status:** Proposed contract outline  
**Implementation status:** No API has been implemented yet

This document defines the REST conventions and endpoint catalogue to use when implementation begins. It is intentionally a contract outline rather than a generated OpenAPI file. Request and response schemas should be finalized alongside the corresponding feature slice.

## 1. API Principles

- Use REST resources for business records and explicit command subresources for state-changing operations.
- Keep URLs stable and independent of JPA entity names.
- Use request and response DTOs; never serialize persistence entities directly.
- Keep business validation in the service layer even when the frontend validates the same fields for usability.
- Return consistent errors and status codes across all modules.
- Document all implemented endpoints, security requirements, examples, and expected failure responses in OpenAPI.
- Keep calculations and inventory mutations on the backend.

## 2. Base Contract

| Concern | Proposed contract |
| --- | --- |
| Base path | `/api/v1` |
| Format | JSON request and response bodies |
| Content type | `application/json` |
| Authentication | `Authorization: Bearer <JWT>` for protected endpoints |
| Identifier format | UUID string |
| Time format | ISO-8601 timestamp in UTC, for example `2026-09-22T12:30:00Z` |
| Decimal quantities | JSON strings such as `"12.500"` to preserve exact decimal values in JavaScript clients |
| Product counts | Positive integer JSON values |
| Character encoding | UTF-8 |
| API versioning | URL major version; additive response fields should remain backward compatible where practical |

The date in the example is illustrative only. The server owns authoritative timestamps for state changes.

## 3. Authentication Contract

### Login

`POST /api/v1/auth/login`

Request:

```json
{
  "email": "planner@example.com",
  "password": "example-password"
}
```

Response `200 OK`:

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresAt": "2026-09-22T13:00:00Z",
  "user": {
    "id": "4c52f5e9-2c11-4a44-9e10-5e3b5cbf8a17",
    "displayName": "Planner",
    "roles": ["PLANNER"]
  }
}
```

The actual example token must never be a real credential. The MVP uses a short-lived access token; refresh-token behaviour is documented in the security design and is not assumed by this contract.

### Current user

`GET /api/v1/auth/me`

Returns the authenticated user and effective roles. This endpoint is useful for initializing the React client after login.

## 4. Resource and Naming Conventions

- Use plural nouns for collections: `/products`, `/materials`, `/customers`, `/customer-orders`, and `/production-orders`.
- Use kebab-case for multi-word resource names.
- Use `POST` for creation and explicit state-changing commands.
- Use `GET` for reads.
- Use `PATCH` only for permitted partial edits to mutable drafts or descriptive master-data fields.
- Avoid generic `PATCH /{id}` operations that allow a client to set arbitrary status values.
- Use nested resources only when the child is meaningful in the context of its parent, such as production stages and order lines.
- Return a stable resource representation after a successful command when the client needs the new state.

## 5. HTTP Status Contract

| Status | Use |
| --- | --- |
| `200 OK` | Successful read or state-changing command with a response body |
| `201 Created` | Successful resource creation; include a `Location` header where practical |
| `204 No Content` | Successful operation with no response body, such as a permitted archive command |
| `400 Bad Request` | Malformed JSON, invalid parameter shape, or boundary validation failure |
| `401 Unauthorized` | Missing, invalid, or expired authentication |
| `403 Forbidden` | Authenticated user lacks the required role or permission |
| `404 Not Found` | Resource does not exist or is not visible to the caller |
| `409 Conflict` | Duplicate identity, stale version, concurrent command, or invalid current-state transition |
| `422 Unprocessable Entity` | Request is syntactically valid but violates a domain rule such as insufficient material or missing active BOM |
| `500 Internal Server Error` | Unexpected failure; response must not expose implementation details |

The final implementation should use one consistent mapping. Clients must not infer business state from a mixture of arbitrary status codes.

## 6. Error Response

Expected errors should use a Problem Details-inspired shape:

```json
{
  "type": "https://example.invalid/problems/insufficient-material",
  "title": "Material reservation failed",
  "status": 422,
  "code": "INSUFFICIENT_MATERIAL",
  "message": "One or more required materials are not available.",
  "path": "/api/v1/production-orders/4c52f5e9-2c11-4a44-9e10-5e3b5cbf8a17/material-reservation",
  "timestamp": "2026-09-22T12:30:00Z",
  "traceId": "a2f9f3c6e9fd4cd2",
  "fieldErrors": []
}
```

Validation errors may include field entries:

```json
{
  "field": "quantity",
  "code": "POSITIVE_REQUIRED",
  "message": "Quantity must be greater than zero."
}
```

Error responses must not include stack traces, SQL, credentials, JWT content, or internal class names.

## 7. Pagination, Filtering, and Sorting

Collection endpoints that can grow should accept a consistent query shape:

```text
?page=0&size=20&sort=createdAt,desc&status=ACTIVE
```

Proposed response shape:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 0,
  "totalPages": 0,
  "first": true,
  "last": true
}
```

The API should expose filters only when they represent a useful business query. It should reject unsupported sort fields rather than silently ignoring them.

## 8. Endpoint Catalogue

The role abbreviations used below are:

- `A`: Administrator
- `P`: Planner
- `I`: Inventory manager
- `O`: Production operator
- `Q`: Quality inspector

Read access can be broader than command access where the security design allows it. The final authorization matrix is defined in `docs/08-security-design.md`.

### 8.1 Identity and access

| Method | Path | Purpose | Roles |
| --- | --- | --- | --- |
| `POST` | `/auth/login` | Authenticate and issue an access token | Public |
| `GET` | `/auth/me` | Return current user and roles | Authenticated |
| `GET` | `/users` | List internal users | A |
| `POST` | `/users` | Create an internal user | A |
| `GET` | `/users/{userId}` | View a user | A |
| `PATCH` | `/users/{userId}` | Update permitted user fields or roles | A |
| `POST` | `/users/{userId}/deactivation` | Deactivate a user | A |

Roles are seeded reference data in the MVP and are not created dynamically through the API.

### 8.2 Products and materials

| Method | Path | Purpose | Roles |
| --- | --- | --- | --- |
| `GET` | `/products` | List products with status/filter support | Authenticated |
| `POST` | `/products` | Create a product | A, P |
| `GET` | `/products/{productId}` | View a product | Authenticated |
| `PATCH` | `/products/{productId}` | Update permitted product fields | A, P |
| `POST` | `/products/{productId}/archival` | Archive a product | A, P |
| `GET` | `/materials` | List materials with status/filter support | Authenticated |
| `POST` | `/materials` | Create a material | A, P |
| `GET` | `/materials/{materialId}` | View a material | Authenticated |
| `PATCH` | `/materials/{materialId}` | Update permitted material fields | A, P |
| `POST` | `/materials/{materialId}/archival` | Archive a material | A, P |

### 8.3 Bill of Materials

| Method | Path | Purpose | Roles |
| --- | --- | --- | --- |
| `GET` | `/products/{productId}/bom-revisions` | List revisions for a product | Authenticated |
| `POST` | `/products/{productId}/bom-revisions` | Create a draft revision | A, P |
| `GET` | `/bom-revisions/{revisionId}` | View a revision and its lines | Authenticated |
| `PATCH` | `/bom-revisions/{revisionId}` | Edit a draft revision | A, P |
| `POST` | `/bom-revisions/{revisionId}/activation` | Activate a valid draft revision | A, P |
| `POST` | `/bom-calculations` | Preview material requirements for a product quantity | A, P |

The calculation endpoint is read-like in business effect, but `POST` is appropriate because the input is a calculation command body rather than a short, stable resource identifier.

Example calculation request:

```json
{
  "productId": "4c52f5e9-2c11-4a44-9e10-5e3b5cbf8a17",
  "quantity": 100
}
```

### 8.4 Inventory

| Method | Path | Purpose | Roles |
| --- | --- | --- | --- |
| `GET` | `/inventory/balances` | List on-hand, reserved, and available balances | Authenticated |
| `GET` | `/inventory/balances/{inventoryItemId}` | View one stock balance | Authenticated |
| `GET` | `/inventory/movements` | Review append-only movement history | Authenticated |
| `POST` | `/inventory/receipts` | Record a stock receipt | A, I |
| `POST` | `/inventory/adjustments` | Record an approved adjustment | A, I |

Reservation and consumption are driven by production commands so that the inventory service remains the owner of stock mutation.

### 8.5 Customers and customer orders

| Method | Path | Purpose | Roles |
| --- | --- | --- | --- |
| `GET` | `/customers` | List customers | Authenticated |
| `POST` | `/customers` | Create a customer | A, P |
| `GET` | `/customers/{customerId}` | View a customer | Authenticated |
| `PATCH` | `/customers/{customerId}` | Update permitted customer fields | A, P |
| `POST` | `/customers/{customerId}/inactivation` | Mark a customer inactive | A, P |
| `GET` | `/customer-orders` | List customer orders | Authenticated |
| `POST` | `/customer-orders` | Create a draft order | A, P |
| `GET` | `/customer-orders/{orderId}` | View an order and lines | Authenticated |
| `PATCH` | `/customer-orders/{orderId}` | Edit a draft order | A, P |
| `POST` | `/customer-orders/{orderId}/confirmation` | Confirm an order | A, P |
| `POST` | `/customer-orders/{orderId}/cancellation` | Cancel an eligible order | A, P |

### 8.6 Production and stages

| Method | Path | Purpose | Roles |
| --- | --- | --- | --- |
| `GET` | `/production-orders` | List production orders | Authenticated |
| `POST` | `/production-orders` | Create a production order from one confirmed order line | A, P |
| `GET` | `/production-orders/{productionOrderId}` | View production status, snapshot, requirements, and stages | Authenticated |
| `GET` | `/production-orders/{productionOrderId}/requirements` | View material requirements | Authenticated |
| `POST` | `/production-orders/{productionOrderId}/material-reservation` | Reserve all material requirements | A, P, I |
| `POST` | `/production-orders/{productionOrderId}/cancellation` | Cancel an eligible production order and release reservation | A, P, I |
| `GET` | `/production-orders/{productionOrderId}/stages` | View ordered stage executions | Authenticated |
| `POST` | `/production-orders/{productionOrderId}/stages/{stageId}/start` | Start the current stage | A, O |
| `POST` | `/production-orders/{productionOrderId}/stages/{stageId}/completion` | Complete the current stage | A, O |

The create request should contain the customer-order-line identifier. The server determines the product, quantity, active BOM, requirement snapshot, and stage route.

### 8.7 Quality and completion

| Method | Path | Purpose | Roles |
| --- | --- | --- | --- |
| `GET` | `/production-orders/{productionOrderId}/quality-inspection` | View the final inspection | Authenticated |
| `POST` | `/production-orders/{productionOrderId}/quality-inspection` | Record one final inspection and defects | A, Q |
| `POST` | `/production-orders/{productionOrderId}/completion` | Complete a passed production order transactionally | A, P |

Completion is an explicit command subresource rather than a generic production-order status update. This keeps the material consumption and finished-stock receipt behind a single server-side invariant.

## 9. Representative DTO Shapes

### Production creation request

```json
{
  "customerOrderLineId": "4c52f5e9-2c11-4a44-9e10-5e3b5cbf8a17"
}
```

### Production response summary

```json
{
  "id": "c5a1a086-5c92-4a1b-9ef9-12fb3ea77445",
  "productionNumber": "PO-000001",
  "customerOrderLineId": "4c52f5e9-2c11-4a44-9e10-5e3b5cbf8a17",
  "product": {
    "id": "e4f4c8e8-6e1f-46e0-b0b7-e29caec0fdd7",
    "code": "TSHIRT-BASIC",
    "name": "Basic T-Shirt"
  },
  "quantity": 100,
  "status": "PLANNED",
  "bomRevision": 3,
  "requirements": [
    {
      "materialId": "6d40f76c-64ce-4f41-96ea-dc0e895d9af4",
      "materialCode": "FABRIC-COTTON",
      "unit": "METER",
      "requiredQuantity": "185.000"
    }
  ],
  "version": 0,
  "createdAt": "2026-09-22T12:30:00Z"
}
```

The response is illustrative. Fields should be finalized with the database and security documents before implementation.

## 10. Concurrency and Duplicate Commands

- The backend is authoritative for inventory availability and applies row-level locking inside the reservation or completion transaction.
- A command sent against an invalid current state returns `409 Conflict` or `422 Unprocessable Entity` according to whether the conflict is state/concurrency or a domain validation failure.
- Mutable aggregate responses should include a `version` value. Commands that edit a draft or stateful record can require the client to send the expected version.
- The MVP does not add a distributed idempotency service. Duplicate state commands must be rejected without duplicate movements. A future external integration can introduce an idempotency key after a concrete retry requirement is identified.

## 11. OpenAPI Requirements

The generated OpenAPI document should include:

- API title, version, description, and scope limitations
- Bearer JWT security scheme
- Every implemented endpoint and operation summary
- Request and response schemas
- Validation constraints and examples
- Success, authentication, authorization, not-found, conflict, and business-error responses
- Pagination and filter parameters
- DTO field descriptions for quantities, units, status values, and timestamps
- Links or references to the relevant use cases and business rules where practical

OpenAPI should be generated from the Spring application and reviewed as part of the API contract, but it should not replace the human-readable Markdown design documents.

## 12. API Design Decisions

| Decision | Why it is appropriate | Alternative rejected | Interview and reengineering benefit |
| --- | --- | --- | --- |
| Use `/api/v1` and resource-oriented URLs | Gives the API a clear evolution boundary and predictable client contract | Expose Java class names or unversioned ad hoc paths | Demonstrates API lifecycle thinking |
| Use explicit command subresources for state changes | Reservation, confirmation, completion, and stage transitions have distinct invariants | Allow generic PATCH requests to set any status | Prevents clients from bypassing workflow rules and makes commands discoverable |
| Use DTOs and stable error envelopes | The API should not be coupled to JPA mappings and clients need predictable failures | Return entities or framework exception shapes directly | Demonstrates separation of concerns and client-oriented design |
| Represent decimal quantities as JSON strings | JavaScript number precision can corrupt material quantities | Return all decimals as JSON numbers | Shows awareness of cross-language numeric correctness |
| Use `409` for state/concurrency conflict and `422` for domain rejection | Clients can distinguish stale state from an otherwise valid but impossible business command | Return `400` for every failure | Makes error handling more precise and testable |
| Keep inventory commands behind production workflows | The inventory module remains the only stock owner while production provides context | Let clients directly edit reserved or consumed quantities | Preserves invariants and auditability |
| Use one major API version rather than per-module versions | The monolith has one coherent client contract | Version every module independently from the start | Avoids unnecessary coordination complexity while preserving a future breaking-change boundary |
| Use bounded pagination and explicit filters | Prevents unbounded list responses without adding a search platform | Return every record or add Elasticsearch early | Demonstrates pragmatic operational design |
