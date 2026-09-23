# Completed Features and Milestones

No application feature has been implemented yet. The following planning milestones are complete.

## DOC-001: Documentation Baseline

- **Type:** Project milestone
- **Evidence:** `README.md` and `docs/01` through `docs/10`

## STR-001: Initial Repository Skeleton

- **Type:** Project milestone
- **Evidence:** `backend/` and `frontend/` structure with no executable implementation

## FND-001: Git Repository and Project Hygiene

- **Type:** Foundation feature
- **Evidence:** Git repository initialized on `main`; `.gitignore` and `.gitattributes` added; branch, commit, and command conventions documented in `README.md`; no secrets, environment files, or generated output tracked.

## FND-002: Spring Boot Backend Bootstrap

- **Type:** Foundation feature
- **Evidence:** `backend/pom.xml` (Spring Boot 3.5.16, Java 21 target, justified dependencies only); main class at the root package for full component-scan coverage; `application.yml` with env-var configuration, `ddl-auto: validate`, `open-in-view: false`, UTC; Maven Wrapper added; smoke test verifies context load and main-class registration (`./mvnw test`, 2/2 passed). Full startup against PostgreSQL is verified in FND-004.

When an application feature is complete, add it only after its acceptance criteria, tests, security review, and documentation are finished. Link to the relevant implementation or test paths as evidence.
