# Architecture Decision Records

Each file records one significant decision: its context, the decision and its
consequences. Accepted records are not rewritten; a changed decision gets a new
record that supersedes the old one.

The decisions are mine, made during design sessions. The records are drafted
with an AI assistant and reviewed and approved by me.

| ADR | Title | Status |
| --- | --- | --- |
| [0001](0001-modular-monolith-with-spring-modulith.md) | Build a modular monolith with Spring Modulith | Accepted |
| [0002](0002-application-generated-uuid-ids.md) | Generate UUID v4 ids in the application | Accepted |
| [0003](0003-database-guardrails-owned-by-flyway.md) | Let Flyway own the schema and put guardrails in the database | Accepted |
| [0004](0004-settlement-iban-is-write-only.md) | Treat the settlement IBAN as write-only | Accepted |
| [0005](0005-status-transitions-as-action-endpoints.md) | Change merchant status through action endpoints | Accepted |
| [0006](0006-problem-details-and-database-uniqueness.md) | Report errors as Problem Details and let the database decide uniqueness | Accepted |