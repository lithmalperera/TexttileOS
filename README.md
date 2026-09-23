# Textile Manufacturing Management System

A portfolio-quality textile manufacturing management system designed as a focused modular monolith. The system will model the flow from a customer order to production, quality inspection, and inventory updates while demonstrating practical backend engineering with Java and Spring Boot.

**Status:** Planning and documentation phase. No backend or frontend functionality has been implemented yet.

## Project Goal

This project is intentionally smaller than a full ERP system. It focuses on a traceable manufacturing workflow that can be built, tested, and explained by one developer:

```text
Customer order
  -> Production order
  -> BOM snapshot and material requirements
  -> Inventory availability check and reservation
  -> Production stages
  -> Quality inspection and defect recording
  -> Production completion
  -> Material consumption and finished-product inventory update
```

The project is intended to demonstrate:

- Relational data modelling with PostgreSQL
- REST API design with DTOs, validation, and OpenAPI documentation
- Spring service-layer business rules and transaction boundaries
- Authentication and role-based authorization with Spring Security and JWT
- Inventory consistency under competing requests
- Automated unit, integration, and workflow testing
- A small React and TypeScript interface for the operational workflow
- Dockerized local development without unnecessary distributed infrastructure

## Proposed Modules

The application will be one deployable Spring Boot application with clear internal module boundaries:

1. Identity and Access (Authentication and Users)
2. Products
3. Materials
4. Bill of Materials (BOM)
5. Inventory
6. Customers
7. Customer Orders
8. Production
9. Production Stages
10. Quality and Defects

The MVP is scoped around a small-batch cut-and-sew manufacturing workflow. The full scope and the responsibilities of each module are described in the [project overview](docs/01-project-overview.md).

## Technology Direction

| Area | Planned technology |
| --- | --- |
| Backend | Java and Spring Boot |
| Frontend | React, TypeScript, and Vite |
| Database | PostgreSQL |
| Persistence | Spring Data JPA and Hibernate |
| Security | Spring Security and JWT |
| API | REST |
| Documentation | Markdown and OpenAPI/Swagger |
| Local environment | Docker Compose |
| Version control | Git and GitHub |
| Architecture | Modular monolith |

Redis, RabbitMQ, Kubernetes, GraphQL, and other optional infrastructure are deliberately deferred. They will only be considered after the core workflow is stable and a concrete requirement justifies them.

## Documentation

### Available now

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

The initial documentation baseline is now complete. These documents should be updated alongside implementation when approved behaviour changes. The [Learning Guide](LEARNING-GUIDE.md) explains each implementation step as it is built.

## Repository Conventions

### Branches

- `main`: always buildable; contains reviewed work.
- Feature branches: `feature/<tracker-id>-short-name`, for example `feature/fnd-002-backend-bootstrap`.
- Documentation-only changes may use `docs/<topic>`.

### Commits

- Use short imperative subjects: `Add backend module skeleton`.
- Reference the tracker ID in the body when one exists, for example `Tracker: FND-001`.
- Do not commit secrets, environment files, build output, IDE files, or generated dependencies. `.gitignore` and `.gitattributes` enforce the baseline.

### Commands

The application is not bootstrapped yet, so there are no build or run commands yet. They will be added here as each foundation feature lands:

- `FND-002` adds backend build and test commands.
- `FND-003` adds frontend install, dev, and type-check commands.
- `FND-004` adds the Docker Compose database commands.
- `FND-005` adds migration and verification commands.

Working with the repository right now only requires Git:

```bash
git status                  # see what changed
git add <files>             # stage specific files; avoid blanket adds
git commit                  # commit staged work with a documented message
git switch -c <branch>      # create a feature branch
```

## Scope Guardrails

- Keep one deployable application and one primary PostgreSQL database.
- Prefer a complete, tested workflow over a large number of shallow features.
- Keep the frontend small and focused on the operational workflow.
- Do not add procurement, accounting, payroll, advanced planning, or multi-site inventory to the MVP.
- Do not begin backend or frontend implementation until the scope and design have been reviewed.
