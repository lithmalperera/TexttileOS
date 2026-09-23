# Security Design

**Project:** Textile Manufacturing Management System
**Document status:** Reduced MVP scope, proposed
**Implementation status:** Security foundation exists; JWT login is planned in `IAM-002`

## 1. Security Goals

- Authenticate internal users with JWT.
- Enforce fixed operational roles on the backend.
- Protect passwords and signing keys.
- Prevent clients from changing stock or workflow state directly.
- Return safe errors and logs.
- Attribute important operational actions to users.

## 2. Authentication

- Users are administrator-created internal accounts.
- Passwords use BCrypt hashes.
- Login returns a short-lived JWT.
- Protected requests use the `Authorization: Bearer` header.
- The backend checks that the user is active and loads authoritative roles.
- Tokens contain identity and expiry claims, not passwords or business data.
- Access tokens are kept in memory by the React client for the MVP.

Refresh tokens, external identity providers, customer authentication, and password-reset workflows are deferred.

## 3. Roles

- `ADMIN`: user management and all operational commands.
- `PLANNER`: catalog, BOM, manufacturing order, production planning, and completion.
- `INVENTORY_MANAGER`: receipts, adjustments, balances, reservations, and release.
- `PRODUCTION_OPERATOR`: fixed-stage transitions.
- `QUALITY_INSPECTOR`: quality submission.

All roles may receive read access according to the endpoint policy. The backend, not the React UI, enforces permissions.

## 4. Authorization Layers

Authorization has two layers:

1. Endpoint-level rules such as `@PreAuthorize("hasRole('ADMIN')")`.
2. Service-level state and ownership validation.

This prevents a future non-HTTP caller from bypassing business security.

## 5. API Security

- Health and OpenAPI paths are public.
- All business paths require authentication after JWT login is implemented.
- Anonymous protected requests return `401`.
- Authenticated users without permission return `403`.
- Request DTOs use explicit fields to prevent mass assignment.
- Status fields cannot be changed through generic PATCH requests.
- SQL uses parameterized JPA/repository queries.
- Collection results are bounded.
- CORS allows only configured frontend origins.
- JWTs are not stored in `localStorage`.

The API uses bearer headers rather than authentication cookies in the MVP, so cookie-based CSRF is not the primary threat. If refresh cookies are added later, CSRF protection must be redesigned.

## 6. Secrets

Environment configuration supplies:

- database credentials
- JWT signing secret
- issuer
- allowed frontend origins

No real credentials belong in source control, Docker Compose defaults, screenshots, or documentation.

## 7. Threats to Test

- invalid and expired JWTs
- disabled users
- planner attempting inventory mutation
- operator attempting completion
- direct API calls bypassing hidden UI controls
- leaked password hashes or tokens in responses/logs
- duplicate inventory commands
- SQL injection through filters or search fields

## 8. Deferred Security Extensions

- rotating refresh tokens
- external identity provider
- customer accounts
- multi-tenant authorization
- distributed rate limiting
- advanced audit log search

## 9. Security Decisions

### Local JWT instead of external identity

- **Decision:** implement local JWT authentication.
- **Why:** it demonstrates the security fundamentals directly and fits an internal portfolio app.
- **Rejected:** OAuth provider integration before a real requirement exists.
- **Benefit:** understandable, testable security boundaries.

### Fixed roles instead of a permission builder

- **Decision:** use five fixed operational roles.
- **Why:** roles map to real responsibilities and are easy to review.
- **Rejected:** dynamic permission administration.
- **Benefit:** least privilege without turning authorization into a separate product.

### No refresh tokens initially

- **Decision:** short-lived access token and re-login after expiry in the MVP.
- **Why:** avoids cookie and revocation complexity.
- **Rejected:** persistent refresh-token storage before the workflow needs it.
- **Benefit:** smaller attack surface with a clear future extension.
