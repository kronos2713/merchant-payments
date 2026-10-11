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
| [0007](0007-injected-clock-at-microsecond-precision.md) | Take all time from one injected clock at microsecond precision | Accepted |
| [0008](0008-money-as-minor-units-in-shared.md) | Model money as minor units in a shared value object | Accepted |
| [0009](0009-expiry-computed-on-read.md) | Compute expiry when the status is read | Accepted |
| [0010](0010-payment-request-lifecycle-rules.md) | Define the payment request lifecycle rules | Accepted |
| [0011](0011-routes-split-by-caller.md) | Split the payment request routes by caller | Accepted |
| [0012](0012-list-endpoint-semantics.md) | Filter the payment request list by logical status | Accepted |