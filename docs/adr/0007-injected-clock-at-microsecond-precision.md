# 0007. Take all time from one injected clock at microsecond precision

Date: 2026-10-11
Recorded retrospectively; decided 2026-10-01 (injected clock) and 2026-10-02 (microsecond precision).
Status: Accepted

## Context

Time drives real behaviour here: a payment request expires at `expires_at`, and
a request paid one microsecond too late must be refused. Tests have to control
time exactly, or the expiry rules can't be tested at their boundaries.

There is also a precision mismatch. A Java `Instant` can carry nanoseconds, but
PostgreSQL `timestamptz` stores microseconds. Early on, a POST response showed
a `createdAt` with nanoseconds, and a GET of the same merchant returned a
different value: the one the database had kept.

## Decision

All application time comes from one `Clock` bean in `shared`:

    Clock.tick(Clock.systemUTC(), Duration.ofNanos(1_000))

- **Injected everywhere.** Factories and transitions take the clock or an
  `Instant` as a parameter, for example `Merchant.register(..., clock)` and
  `PaymentRequest.create(..., clock)`. The main code never calls
  `Instant.now()`.
- **UTC,** stored as `timestamptz`.
- **Microsecond ticks,** so every instant the application creates is one that
  PostgreSQL can store exactly. What a response shows is what a later read
  returns.
- **The domain doesn't rely on the bean alone.** Where a value comes from
  outside, it is truncated to microseconds in the domain itself: a client's
  `expiresAt`, and since the review fix also the default expiry in
  `ExpiryPolicy`.

Tests pass `Clock.fixed(...)` and can place an instant exactly on, just before
or just after a boundary.

### Alternatives considered

- **`Instant.now()` where it is needed.** Simplest, but time becomes a hidden
  dependency. Expiry boundaries can't be tested without sleeping or mocking
  static methods.
- **Truncate in each entity, for example in a `@PrePersist` callback.** Works,
  but the rule is spread over every entity, and a new entity can forget it. The
  value in memory before the save would still differ from the stored one.
- **Live with the mismatch.** It shows up as responses that disagree with later
  reads, and as `equals` checks on timestamps that fail in tests.

## Consequences

- Responses and later reads agree on every timestamp.
- Time-based rules are tested at their exact boundaries, without sleeps.
- Two calls within the same microsecond get the same instant. That is harmless:
  lists sort by `createdAt` and then by `id`, so ties have a stable order.
- Passing the clock around is a convention that code review enforces. A test
  rule that fails the build on `Instant.now()` in main code would make it
  automatic, and is a possible later addition.