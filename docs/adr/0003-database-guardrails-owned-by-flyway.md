# 0003. Let Flyway own the schema and put guardrails in the database

Date: 2026-10-11
Recorded retrospectively; decided 2026-10-01.
Status: Accepted

## Context

The schema needs one owner, and every change to it should be reviewable. The
Java code validates input, but Java is not the only way data reaches a table: a
bug, a future batch job or a manual SQL fix can bypass it. For payment data I
want the database itself to refuse values that make no sense.

## Decision

Flyway owns the schema. Every change is a versioned, plain SQL migration in
`db/migration` (`V1__create_merchants.sql`, `V2__create_payment_requests.sql`).
Hibernate runs with `ddl-auto: validate`: at startup it checks that the entities
match the tables, but it never changes the schema.

The tables carry their own guardrails:

- **Bounded `varchar(n)` instead of `text`,** sized from the domain: IBAN 34
  characters, description 140 (the SEPA remittance limit), merchant reference 35
  (the SEPA end-to-end id length).
- **Check constraints for the rules that must always hold:** a positive amount,
  EUR only, the status values, `paid_at` set exactly when the status is PAID,
  `expires_at` after `created_at`, and the shape of a normalised IBAN and
  registration number.
- **Status as `varchar(16)` with a check constraint,** not a PostgreSQL enum type.
- **Every constraint has a name** with a prefix: `ck_`, `uq_`, `fk_`. The code
  relies on this: a duplicate is detected by the name of the unique constraint
  it violated (ADR 0006).

The same rules also exist in Java, so a client gets a clear 400 before anything
reaches the database. The database is the last line, not the first.

### Alternatives considered

- **Hibernate `ddl-auto: update`.** No migration files to write, but the schema
  changes silently on startup, nothing is reviewed, and it can't express check
  constraints or data changes. Fine for a prototype, not for payment data.
- **Liquibase.** More features, such as generated rollbacks and changelogs in
  several formats. Flyway's plain SQL files are simpler, and plain SQL is what I
  read and write every day.
- **PostgreSQL enum types for status.** Type-safe in the database, but a value
  can't simply be removed from an enum type later, while a check constraint is
  replaced in one migration. With `@Enumerated(EnumType.STRING)` the mapping to
  `varchar` is also the simplest one.
- **Validation in Java only.** Less duplication, but nothing protects the data
  from code paths that don't go through the domain.

## Consequences

- The schema history is in git, and every environment, including Testcontainers
  in CI, builds the same schema from the same files.
- A mapping mistake in an entity fails at startup, not at the first query.
- Rules live in two places, Java and SQL. Changing one, for example allowing a
  second currency, needs a code change and a migration together.
- Migrations are forward-only: a mistake is fixed with a new migration, never by
  editing an applied one.