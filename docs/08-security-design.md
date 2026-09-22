# Security Design

**Project:** Textile Manufacturing Management System  
**Document status:** Proposed  
**Implementation status:** Documentation only

The MVP is an internal operational application. It needs credible authentication, authorization, input protection, secret handling, and audit behaviour without becoming an identity-management product.

## 1. Security Goals

- Allow only authenticated internal users to access operational data.
- Enforce least-privilege roles on the backend.
- Protect passwords, JWT signing material, and database credentials.
- Prevent users from bypassing workflow and inventory rules by calling the API directly.
- Avoid leaking sensitive details through errors, logs, or API responses.
- Make important inventory and production actions attributable to a user and time.
- Keep the security model small enough to test completely as a solo project.

## 2. Trust Boundaries

```text
+-----------------------+       HTTPS       +--------------------------+
| Browser and React UI  | <----------------> | Spring Boot API         |
| Untrusted client      |                    | Authentication boundary |
+-----------------------+                    +------------+-------------+
                                                          |
                                                          | server-side credentials
                                                          v
                                             +------------+-------------+
                                             | PostgreSQL              |
                                             | private application DB  |
                                             +--------------------------+
```

The browser is not trusted to enforce roles, calculate requirements, or maintain inventory. The API is the security boundary for business operations. PostgreSQL credentials and JWT signing keys are available only to the backend process.

## 3. Authentication Model

### 3.1 Local user accounts

- Users are created by an administrator; there is no public self-registration.
- A user has a normalized login email, display name, status, password hash, and one or more seeded roles.
- Customers are business records and do not authenticate in the MVP.
- Password hashes use Spring Security's BCrypt password encoder with a configurable work factor appropriate to the deployment.
- The backend never stores or returns plaintext passwords.

### 3.2 Login flow

```text
Client submits email/password
  -> API normalizes identifier
  -> Spring Security verifies BCrypt hash
  -> API confirms user is active
  -> API issues short-lived signed JWT access token
  -> Client sends token in Authorization header
```

Invalid credentials and inactive accounts return the same general authentication failure shape. The response must not reveal which part of the credentials failed.

### 3.3 JWT access token

The MVP uses a short-lived access token. A target default is 15 minutes, with the value supplied through configuration rather than hard-coded into business code.

The token may contain:

- `iss`: configured issuer
- `sub`: user UUID
- `iat`: issued-at time
- `exp`: expiry time
- `jti`: token identifier if revocation/audit support requires it
- `roles`: current roles for client display and initial authorization context

The backend should reload the user's active status and authoritative current roles when building the authenticated principal. This means deactivation or role removal takes effect without waiting for a long token lifetime. The tradeoff is one user-state lookup for protected requests, which is acceptable for the MVP's scale.

JWTs must not contain passwords, password hashes, secrets, full customer data, or unnecessary personal data.

### 3.4 Token signing

Because the MVP has one token issuer and one API verifier in one deployable application, an HMAC-SHA-256 signing key supplied through an environment variable is sufficient. The key must be long, random, absent from source control, and different across environments.

If the system later adds independently deployed services or external token verification, an asymmetric signing strategy such as RSA or ECDSA should be reconsidered.

### 3.5 Token storage in the frontend

The preferred MVP approach is to keep the access token in memory and require login again after a full browser refresh. The client should not put bearer tokens in `localStorage`, because a successful cross-site scripting attack could persistently read them.

Rotating refresh tokens in secure, HttpOnly cookies are a possible future improvement if the user experience requires long-lived sessions. They are not required for the first internal workflow demonstration.

## 4. Authorization Model

The MVP uses a small fixed role set:

| Role | Responsibility |
| --- | --- |
| `ADMIN` | User administration and all operational capabilities |
| `PLANNER` | Product, material, BOM, customer, order, and production planning |
| `INVENTORY_MANAGER` | Stock receipts, adjustments, availability, reservations, and safe reservation release |
| `PRODUCTION_OPERATOR` | Starting and completing the current production stage |
| `QUALITY_INSPECTOR` | Recording final inspections and defects |

### 4.1 Authorization matrix

`R` means read, `C` means create or command, and `-` means not permitted by default.

| Operation | ADMIN | PLANNER | INVENTORY_MANAGER | PRODUCTION_OPERATOR | QUALITY_INSPECTOR |
| --- | ---: | ---: | ---: | ---: | ---: |
| Authenticate | R | R | R | R | R |
| Manage users and roles | C | - | - | - | - |
| Read products/materials/BOMs | R | R | R | R | R |
| Create or edit products/materials/BOMs | C | C | - | - | - |
| Manage customers and customer orders | C | C | - | - | - |
| Read inventory balances and movements | R | R | R | R | R |
| Receive or adjust inventory | C | - | C | - | - |
| Create production order | C | C | - | - | - |
| Reserve or release production materials | C | C | C | - | - |
| Read production orders and stages | R | R | R | R | R |
| Start or complete production stages | C | - | - | C | - |
| Record quality inspection | C | - | - | - | C |
| Complete quality-approved production | C | C | - | - | - |
| Cancel eligible production | C | C | C | - | - |

The matrix is an initial least-privilege proposal. A later security review may split read permissions or restrict sensitive operations further, but the MVP should not create a new role for every screen.

### 4.2 Server-side enforcement

Authorization should exist at two levels:

1. **Endpoint-level authorization:** Spring Security checks that the authenticated principal has a role permitted for the route.
2. **Service-level authorization and state validation:** The service checks the business command, current state, and any object-level rule before mutation.

Endpoint annotations alone are not enough. A service method must remain safe if it is called from another controller, scheduled task, or integration test.

## 5. API Security Rules

- Require bearer authentication for all business endpoints except login and intentionally public health metadata.
- Configure CORS with explicit development and deployment origins. Do not use a wildcard origin with credentials.
- Validate request DTOs before business processing, then apply domain validation in services.
- Reject unknown or unsafe fields where mass assignment could change protected state.
- Do not allow generic status-field patches for inventory, production, quality, or order lifecycles.
- Use parameterized repository queries and Spring Data bindings; never concatenate user input into SQL.
- Return `404` for resources that the caller is not allowed to distinguish from nonexistent resources when that is the safer policy.
- Apply bounded pagination to collection endpoints.
- Limit request body sizes and validate uploaded content if file support is ever added; file uploads are outside the MVP.

## 6. CSRF and Browser Controls

The MVP sends JWTs in the `Authorization` header rather than authentication cookies. This avoids the primary cookie-based CSRF flow, but it does not remove the need for:

- strict CORS configuration
- HTTPS outside local development
- secure response headers
- output encoding and React's normal XSS protections
- avoiding token persistence in browser storage

If refresh tokens or authentication cookies are added later, CSRF protection must be redesigned and enabled for those state-changing requests.

## 7. Secrets and Environment Configuration

The following values must come from environment-specific configuration:

- database username and password
- database host and connection settings
- JWT signing key and issuer
- allowed frontend origins
- optional logging and token-expiry settings

Local examples should use clearly fake values or an ignored `.env` file. No real secret belongs in the repository, Docker Compose defaults, screenshots, or Markdown examples.

## 8. Password and Account Rules

- Require a minimum password length appropriate to the selected security policy, with 12 characters as the proposed MVP baseline.
- Do not require arbitrary composition rules that encourage predictable passwords; length and a password manager are more useful.
- Never log login passwords or authentication headers.
- Use generic login failures.
- Deactivate accounts instead of deleting users that appear in audit records.
- Do not implement password reset email infrastructure in the MVP. An administrator can set a replacement password through a controlled operation until a real deployment requirement exists.
- Consider basic login throttling as a hardening task. Do not add Redis solely for throttling in the first release.

## 9. Audit and Sensitive Data

The following actions should record actor and timestamp:

- user creation, role change, and deactivation
- product, material, BOM activation, order, and production state changes where useful
- inventory receipts, adjustments, reservations, releases, consumption, and finished-stock receipts
- quality inspection submission

The inventory movement ledger is the authoritative audit trail for stock. Application logs support diagnosis but are not a replacement for business records.

Customer contact data should be limited to fields required by the MVP. API responses should not expose internal authentication fields or unrelated user data.

## 10. Threats and Mitigations

| Threat | Mitigation |
| --- | --- |
| Credential theft | BCrypt hashes, HTTPS outside local development, no plaintext logging, short-lived tokens |
| Stolen access token | Short expiry, in-memory browser storage, active-user check, logout by clearing client token |
| Privilege escalation | Fixed roles, backend endpoint and service checks, authorization tests |
| IDOR/resource guessing | UUID identifiers, authorization checks, safe not-found behaviour, no customer portal in MVP |
| Mass assignment | Explicit DTO fields and command-specific service methods |
| Inventory tampering | Inventory module ownership, transactional commands, append-only movements, row locks |
| SQL injection | Parameterized JPA/repository queries and input validation |
| JWT key leakage | Environment-provided secret, ignored local configuration, no key in source control |
| Error or log disclosure | Shared safe error mapping and sensitive-data logging rules |
| Browser XSS/token persistence | React escaping, CSP/security headers where practical, in-memory access token |
| Brute-force login | Generic errors, password policy, basic throttling hardening; distributed rate limiting deferred |
| CSRF after future cookie use | Revisit CSRF before introducing refresh cookies |

## 11. Security Verification Requirements

Security tests must verify at least:

- unauthenticated requests are rejected
- expired or malformed tokens are rejected
- deactivated users cannot authenticate or call protected operations under the chosen active-user policy
- each role can perform permitted commands and is denied prohibited commands
- direct HTTP calls cannot bypass frontend-only restrictions
- error responses do not expose credentials or stack traces
- repository queries do not accept unsafe dynamic SQL
- CORS allows only configured origins
- inventory commands preserve authorization and transaction invariants

## 12. Security Decisions

| Decision | Why it is appropriate | Alternative rejected | Interview and reengineering benefit |
| --- | --- | --- | --- |
| Use local JWT authentication for internal users | Matches the SPA/API boundary and demonstrates authentication fundamentals | External identity provider or session-only server-rendered auth | Keeps the security flow understandable and testable in a solo portfolio project |
| Use short-lived access tokens without refresh tokens initially | The MVP has no long-lived external session requirement and can avoid browser cookie complexity | Add rotating refresh-token persistence before it is needed | Reduces attack surface and implementation scope while documenting a clear extension path |
| Use BCrypt password hashing | It is well-supported by Spring Security and straightforward to configure | Store hashes with a weak or custom algorithm, or add an unsupported custom scheme | Demonstrates standard secure credential handling without unnecessary dependencies |
| Use one fixed role set | Roles map to real operational responsibilities | Dynamic permission builder or per-screen roles | Shows least privilege while keeping authorization reviewable |
| Check current user status and roles server-side | Deactivation and role changes should take effect predictably | Trust token claims until token expiry | Demonstrates revocation thinking and avoids stale authorization |
| Use an environment-provided HMAC key for one issuer | One backend both issues and validates tokens; asymmetric key management is not required yet | Commit a key or operate a full key-distribution system | Shows proportional cryptographic architecture and a future migration path |
| Prefer in-memory access-token storage in the browser | Reduces persistence of bearer tokens under XSS compared with localStorage | Persist access tokens in localStorage for convenience | Demonstrates practical browser security tradeoffs |
| Keep authorization in both endpoint and service layers | Commands remain safe across HTTP and future entry points | Rely only on frontend or controller annotations | Makes security resilient to new callers and refactoring |
