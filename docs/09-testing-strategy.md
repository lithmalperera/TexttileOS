# Testing Strategy

**Project:** Textile Manufacturing Management System
**Document status:** Reduced MVP scope, proposed
**Implementation status:** Testcontainers integration base is active

The testing strategy favors evidence for risky behaviour over broad shallow coverage.

## 1. Required Test Layers

### Unit tests

Cover:

- BOM quantity calculation
- catalog validation
- fixed production-stage transitions
- quality gate rules
- inventory availability decisions
- role policy decisions

### PostgreSQL integration tests

Use the existing Testcontainers PostgreSQL base for:

- Flyway migrations
- JPA mappings
- constraints and unique indexes
- repositories and pagination
- inventory balance updates
- append-only movement history
- rollback behaviour

H2 is intentionally not used for persistence tests.

### API/security tests

Cover:

- DTO validation and error shape
- `401` anonymous requests
- `403` authenticated users with wrong roles
- `201`, `404`, `409`, and `422` outcomes
- DTOs not exposing password hashes
- OpenAPI endpoint availability

### Workflow tests

One end-to-end service/API test should execute:

```text
catalog -> stock receipt -> manufacturing order
-> production -> reservation -> stages
-> quality pass -> completion -> movement review
```

## 2. Must-Have Risk Tests

- Two competing reservations: exactly one succeeds.
- Multi-material reservation: shortage on one line leaves every line unreserved.
- Completion rollback: material consumption, finished receipt, and statuses all roll back on failure.
- Failed quality: completion creates no completion movements.
- BOM snapshot: later BOM edit does not change production requirements.
- Stage state: skipping and repeating fixed stages is rejected.
- Security: role without permission receives `403`.
- Duplicate identity: database uniqueness becomes a documented `409`.

## 3. Frontend Tests

The thin React client needs:

- login success/failure
- validation and server error display
- reservation failure display
- stage progression rendering
- quality failure preventing completion
- decimal quantities displayed without rounding errors

One browser smoke test is useful after the API is stable, but a large browser suite is deferred.

## 4. Fixtures

- Seed only fixed roles and stage values through migrations.
- Build business fixtures in test code.
- Use fake users and customers.
- Never share mutable state between tests without an explicit reason.
- Let Testcontainers create a clean PostgreSQL schema for integration runs.

## 5. Completion Gates

A feature is test-complete when:

- core service rules have unit tests
- persistence-sensitive behaviour has PostgreSQL tests
- authorization is tested
- failure and rollback paths are tested
- API errors and status codes are tested
- the feature's requirement and business-rule references are updated

Do not chase universal 100 percent coverage. Protect the high-risk business rules first.

## 6. Deferred Testing Work

- exhaustive CRUD coverage for deferred customer/order modules
- browser coverage for every screen
- performance/load testing
- distributed messaging tests
- warehouse, lot, serial, and scheduling scenarios

## 7. Testing Decisions

### Real PostgreSQL instead of H2

- **Decision:** Testcontainers PostgreSQL for persistence tests.
- **Why:** inventory locks, constraints, migrations, and SQL semantics matter.
- **Rejected:** H2-only integration tests.
- **Benefit:** avoids false confidence from a different database.

### Risk-based coverage

- **Decision:** prioritize concurrency, rollback, security, and state tests.
- **Why:** these are the project's strongest claims.
- **Rejected:** equal coverage effort for every getter and CRUD path.
- **Benefit:** less time spent on low-risk code and stronger interview evidence.
