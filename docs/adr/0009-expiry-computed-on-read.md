# 0009. Compute expiry when the status is read

Date: 2026-10-11
Recorded retrospectively; decided 2026-10-02 (expiry on read) and 2026-10-03 (no write on a failed attempt).
Status: Accepted

## Context

A payment request has an `expires_at`. After that moment it must not be paid
or cancelled, and every response must show it as EXPIRED. Something has to
decide when a CREATED request becomes EXPIRED, and the answer has to be right
at the exact moment someone asks, not a few minutes later.

## Decision

**The stored status stays CREATED; the expiry is computed when the status is
read.** `PaymentRequest` has no `getStatus()`. The only way to ask for the
status is `statusAt(now)`, which returns EXPIRED for a CREATED request once
`now` is at or past `expires_at`. A request is payable strictly before
`expires_at`.

Removing the getter is deliberate. With a plain `getStatus()`, any caller could
read the stored CREATED of an expired request and act on the wrong answer.
Having to pass `now` makes every caller say when it is asking.

`pay` and `cancel` both decide on `statusAt(now)`. On an expired request they
throw a `PaymentRequestConflictException` (409) and write nothing.

Earlier I planned to store EXPIRED when someone tried to pay an expired
request. I dropped that: the attempt ends in an exception, and `@Transactional`
rolls back on it, so the write would be lost. Keeping it would need
`noRollbackFor` or a separate transaction, which is extra complexity to store a
value the code can compute anyway.

**Queries follow the same rule.** "Open" means `status = 'CREATED' AND
expires_at > now`, never just `status = 'CREATED'` (ADR 0012).

**The expiry itself** is set by the merchant within limits from
`application.yml`: at least 5 minutes, at most 30 days, 24 hours when omitted.
`ExpiryPolicy` binds these limits, and `PaymentRequest.create(...)` applies
them, so no code path can create a request with an out-of-range expiry.

### Alternatives considered

- **A scheduled job that stores EXPIRED.** The database stays honest for direct
  SQL, but between two runs an expired request still looks payable. The read
  check would be needed anyway, so the job alone doesn't solve the problem.
- **Store EXPIRED on a failed pay attempt.** Lost to the rollback, as described
  above.
- **A generated column or a database trigger.** A PostgreSQL generated column
  can't depend on the current time, and a trigger only runs when the row is
  written, not when time passes.
- **Keep `getStatus()` and add `isExpired(now)`.** Less unusual, but nothing
  forces a caller to use the second method.

## Consequences

- No background job in the MVP, and the status is exact at the instant it is
  asked for.
- The boundary is tested at the microsecond: CREATED one microsecond before
  `expires_at`, EXPIRED at it.
- The `status` column alone is misleading: an expired request still says
  CREATED. Anything that reads the table directly, such as a report, must also
  look at `expires_at`.
- A scheduled sweep that stores EXPIRED is on the post-MVP hardening list. The
  EXPIRED filter in the list endpoint already matches stored EXPIRED, so the
  sweep needs no query change.