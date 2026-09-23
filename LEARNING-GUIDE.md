# Learning Guide

This guide explains everything the project builds, step by step: what each piece is, why it exists, and what it teaches you. Read it alongside the code. It is updated after every implementation step.

- **What:** the concept or file we just created
- **Why:** the engineering reason it exists
- **Learn:** what to be able to explain in an interview

This guide is a teaching record, not a replacement for the design documents in `docs/`.

## How the project is built

Each feature goes through the same loop:

1. Pick the next item from `feature-tracker/backlog.md`.
2. Understand its acceptance criteria before writing code.
3. Implement the smallest complete version.
4. Verify it (build, test, run).
5. Update this guide and the feature tracker.
6. Commit with a message that references the tracker ID.

We always build a stable foundation before adding behavior on top of it.

## Milestone: Documentation baseline (completed)

Before any code, the project wrote ten design documents in `docs/`. This is deliberate.

- **What:** requirements, use cases, business rules, architecture, database, API, security, and testing documents.
- **Why:** scope control. A solo project fails by growing too big, not by being built too slowly. The documents decide what is in and out before code exists.
- **Learn:** you can explain the MVP boundary in one sentence: *a confirmed customer-order line becomes one full-quantity production order that reserves materials atomically, passes a quality gate, and completes with an all-or-nothing inventory update.*

## FND-001: Git repository hygiene (completed)

### `git init` on `main`

- **What:** turns the folder into a Git repository with a single branch called `main`.
- **Why:** version control from the first day. Every later change is reviewable and revertable.
- **Learn:** `main` always stays buildable; work happens on `feature/<tracker-id>-name` branches.

### `.gitignore`

- **What:** a list of files Git must never track: `.env` secrets, IDE folders, Maven `target/`, Node `node_modules/`, logs, OS junk.
- **Why:** build output and secrets must never enter history. Once committed, a secret is considered leaked forever.
- **Learn:** be able to name three things your `.gitignore` protects and why each is dangerous to commit.

### `.gitattributes`

- **What:** rules for how Git treats files: line endings (`LF` for shell scripts), binary files (`*.jar`), and diff behavior (`*.md`).
- **Why:** Windows uses `CRLF` line endings, macOS/Linux use `LF`. Without this rule, files show fake "whole file changed" diffs and scripts break.
- **Learn:** the difference: `.gitignore` decides *which* files are tracked; `.gitattributes` decides *how* tracked files behave.

## FND-002: Spring Boot backend bootstrap (in progress)

The goal: a Spring Boot application that builds, starts, and is testable — with zero business logic. A stable shell that everything else will be built on.

### Step 1: `pom.xml`

- **What:** the Maven build file. Declares the Java version, Spring Boot version, and every dependency.
- **Why:** Maven is the single source of truth for "what this project needs to compile and run". Anyone can rebuild the project from this one file.
- **Dependencies, each justified:**
  - `spring-boot-starter-web` — REST API layer (embedded Tomcat, Spring MVC, JSON). We are building an API-first backend.
  - `spring-boot-starter-data-jpa` — Spring Data JPA and Hibernate for persistence. Declared now, used by later modules.
  - `spring-boot-starter-validation` — bean validation for DTOs at the API boundary.
  - `spring-boot-starter-security` — required soon for JWT login; declared with the foundation, not bolted on later.
  - `postgresql` — the PostgreSQL JDBC driver. The database we committed to in the design.
  - `flyway-core` + `flyway-database-postgresql` — versioned database migrations. Schema changes become reviewable source files.
  - `spring-boot-starter-test` — JUnit 5, AssertJ, Spring test support. Testing is part of the foundation, not an afterthought.
  - `spring-security-test` — lets later security tests authenticate fake users.
- **Deliberately excluded:** Redis, RabbitMQ, GraphQL, Kubernetes clients, extra utility libraries. Every dependency is a liability; the design documents say what is deferred.
- **Learn:** for every starter, one sentence: what it brings and which module will use it. Interviewers probe for "did you add this or copy it?"

### Step 2: `TextileManufacturingApplication.java`

- **What:** the main class, annotated `@SpringBootApplication`.
- **Why:** this is the entry point. The annotation is three annotations in one:
  - `@SpringBootConfiguration` — marks this class as a configuration source.
  - `@EnableAutoConfiguration` — Spring inspects the classpath and configures what it finds (JPA found + PostgreSQL driver found = datasource setup).
  - `@ComponentScan` — finds our `@RestController`, `@Service`, `@Repository` classes under this package.
- **Consequence:** the class must live at the root package (`com.textile.manufacturing`) so component scanning covers every module package we planned.
- **Learn:** explain what auto-configuration does and why the main class placement matters.

### Step 3: `application.yml`

- **What:** external configuration: server port, database connection, JPA/Hibernate behavior, Flyway settings.
- **Why:** separates *what the code does* from *where it runs*. Local Docker PostgreSQL and a future deployed database differ only by configuration.
- **Key choices:**
  - `ddl-auto: validate` — Hibernate may *check* the schema but never *change* it. Migrations own the schema.
  - Profiles: `application-local.yml` for development defaults; real secrets stay in environment variables.
- **Learn:** why `ddl-auto: update` is rejected in a real project (silent, unreviewable schema drift).

### Step 4: first smoke test

- **What:** one test that starts the Spring context and asserts the application bean exists.
- **Why:** proves all configuration is valid. Every later feature inherits this safety net; a broken configuration fails in seconds, not at runtime.
- **Learn:** what "the Spring context loads" actually verifies: beans wire, configuration parses, auto-configuration succeeds.

### Step 5: verification

- **What:** `./mvnw clean verify` passes; the app starts and logs cleanly.
- **Why:** the definition of "bootstrap complete" is mechanical, not a feeling.
- **Verified outcome (FND-002):** `./mvnw test` passed 2/2. The context loaded with the `test` profile, and the main-class bean was found by component scan.
- **Observed on purpose:**
  - Spring Security generated a default password and locked all endpoints because no security configuration exists yet. Deny-by-default is the safe direction; the IAM phase replaces this.
  - The `test` profile excludes DataSource/JPA/Flyway auto-configuration because no database exists until FND-004. This scaffolding is removed in FND-005 when Testcontainers brings real PostgreSQL into tests.
  - Mockito dynamic-agent warnings are known noise, not defects.

### Deliberately not in FND-002

No entities, no controllers, no security filters, no business modules. Those belong to their own tracker items. Building them now would mean debugging configuration and business logic at the same time.

## Rules for this guide

- Every completed step gets an entry: what, why, learn.
- Entries are written after verification, not before.
- If a later decision reverses an earlier one, add a note under the old entry explaining why. Do not silently rewrite history.

## Rules for how we work

- The AI creates files, explains decisions, and prepares commands.
- The developer runs every build, test, and verification command personally, and reads the output.
- Verification is only trusted once the developer has seen it pass with their own command run.
