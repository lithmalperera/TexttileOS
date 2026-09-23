# Textile Manufacturing Management System

A focused portfolio project that demonstrates backend engineering through a small textile manufacturing workflow. The application is a modular monolith using Java, Spring Boot, PostgreSQL, JPA/Hibernate, JWT security, REST, Docker Compose, and a small React client.

**Current status:** Foundation complete; identity feature in progress. The application is being built incrementally and verified by the developer at every step.

## MVP Goal

The MVP deliberately reduces business breadth while keeping technical depth:

```text
JWT login
  -> Product, material, and simple BOM setup
  -> Receive material stock
  -> Create one-product manufacturing order
  -> Create production order and snapshot BOM
  -> Reserve materials with database locking
  -> Progress through fixed production stages
  -> Record quality pass/fail
  -> Complete production transactionally
  -> Consume materials and receive finished product
```

The project is intended to demonstrate:

- Relational modelling with PostgreSQL
- Spring Boot REST APIs with DTOs and validation
- JPA/Hibernate mappings and Flyway migrations
- JWT authentication and role-based authorization
- Transaction boundaries and inventory concurrency control
- Automated unit, integration, rollback, and concurrency tests
- Dockerized development
- A small React and TypeScript client that consumes the finished API

## Four Logical Areas

The application remains one deployable modular monolith. The original broader business areas are grouped into four logical areas so the project can be completed and explained by one developer.

### Identity and Access

Users, fixed roles, BCrypt password hashing, JWT login, and backend authorization.

### Catalog

Products, materials, and one simple active BOM per product. Production stores an immutable BOM snapshot.

### Inventory

One logical warehouse, stock balances, receipts, adjustments, movement history, reservations, consumption, and finished-product receipts.

### Manufacturing

The single-product manufacturing order, production order, fixed stage state machine, quality pass/fail, and transactional completion.

## Deferred Extensions

These features were part of the original broader design. They are deliberately deferred, not forgotten:

- Customer master data and multi-line customer orders
- Full BOM revision lifecycle, alternate materials, and multi-level BOMs
- Separate configurable production-stage definitions and execution history
- Detailed defect records, rework, and repeat inspection
- Multiple warehouses, lots, serial numbers, and barcode scanning
- Supplier purchasing, accounting, costing, scheduling, and machine integration
- Customer portal, notifications, refresh tokens, and external identity providers
- Redis, RabbitMQ, Kubernetes, GraphQL, and microservices

The extension rationale is recorded in [`docs/01-project-overview.md`](docs/01-project-overview.md) and the feature tracker.

## Technology Direction

- Backend: Java and Spring Boot
- Frontend: React, TypeScript, and Vite
- Database: PostgreSQL
- Persistence: Spring Data JPA and Hibernate
- Security: Spring Security and JWT
- API: REST
- Migrations: Flyway
- Documentation: Markdown and OpenAPI/Swagger
- Local environment: Docker Compose
- Version control: Git and GitHub
- Architecture: Modular monolith

## Documentation

- [01 - Project Overview](docs/01-project-overview.md)
- [02 - Requirements](docs/02-requirements.md)
- [03 - Use Cases](docs/03-use-cases.md)
- [04 - Business Rules](docs/04-business-rules.md)
- [05 - System Architecture](docs/05-system-architecture.md)
- [06 - Database Design](docs/06-database-design.md)
- [07 - API Specification](docs/07-api-specification.md)
- [08 - Security Design](docs/08-security-design.md)
- [09 - Testing Strategy](docs/09-testing-strategy.md)
- [10 - Development Plan](docs/10-development-plan.md)
- [Feature Tracker](feature-tracker/README.md)
- [Learning Guide](LEARNING-GUIDE.md)

Update the relevant document whenever an approved scope decision changes. The Learning Guide explains each implementation step and the reasoning behind it.

## Current Commands

### Backend

```bash
cd backend
./mvnw test
./mvnw spring-boot:run
```

### Frontend

```bash
cd frontend
npm install
npm run dev
npm run typecheck
npm run build
```

### PostgreSQL

```bash
cp .env.example .env
docker compose up -d
docker compose down
```

### API tools

- Health: `http://localhost:8080/actuator/health`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- Tables: `docker compose exec postgres psql -U textile -d textile -c '\dt'`

## Repository Conventions

- `main` contains reviewed, buildable work.
- Feature branches use `feature/<tracker-id>-short-name`.
- Documentation-only branches use `docs/<topic>`.
- Commit subjects are short and imperative.
- Do not commit secrets, environment files, build output, IDE files, or generated dependencies.
