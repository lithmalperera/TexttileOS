# Feature Tracker

This folder keeps the project scope explicit. The active MVP uses four logical areas: Identity, Catalog, Inventory, and Manufacturing. Broader features remain documented as deferred extensions instead of silently expanding the build.

## Files

- `backlog.md`: approved features that should be implemented
- `in-progress.md`: features currently being worked on
- `completed.md`: features and project milestones that are finished
- `ideas.md`: AI-assisted and human-generated ideas awaiting a decision
- `feature-template.md`: template for documenting one feature before implementation

The active implementation order is in `backlog.md`. Deferred extensions are recorded there and in `ideas.md` so future work has a starting point without becoming current scope.

## Feature Status Flow

```text
IDEA -> PLANNED -> IN_PROGRESS -> REVIEW -> COMPLETED
          |           |
          +---------> DEFERRED
          +---------> REJECTED
```

- **IDEA:** A possibility, not a commitment.
- **PLANNED:** Approved for the current roadmap and recorded in `backlog.md`.
- **IN_PROGRESS:** Actively being implemented; it must appear in `in-progress.md`.
- **REVIEW:** Implementation exists and is being checked against requirements, tests, security, and documentation.
- **COMPLETED:** Acceptance criteria, tests, documentation, and review are complete.
- **DEFERRED:** Valuable but intentionally postponed.
- **REJECTED:** Decided against because it does not support the project goals or MVP scope.

## AI-Assisted Feature Process

AI can suggest features, alternatives, acceptance criteria, and implementation risks. It should not silently decide the product scope.

1. Put an AI-generated suggestion in `ideas.md` first.
2. Write down the user problem and the engineering value it provides.
3. Check the suggestion against the MVP boundaries in `docs/01-project-overview.md` and `docs/04-business-rules.md`.
4. Decide explicitly: move it to the backlog, defer it, or reject it.
5. Link the feature to requirement, use-case, business-rule, API, and test documentation where relevant.
6. Implement only features with a clear acceptance definition.
7. Record what AI suggested and what you changed or rejected in the feature record.

## Feature Quality Gate

Before a feature moves from `IDEA` to `PLANNED`, it should answer:

- What user or operational problem does it solve?
- Which actor uses it?
- Why is it valuable for this portfolio project?
- Which module owns it?
- What is explicitly out of scope?
- Which data, API, security, and testing changes are expected?
- What are the acceptance criteria?
- What simpler alternative was rejected?
- What evidence will prove it is complete?

The tracker does not replace the design documents. It connects an implementation task to those documents so the scope and reasoning remain visible.
