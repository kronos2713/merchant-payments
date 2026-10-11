# 0006. Report errors as Problem Details and let the database decide uniqueness

Date: 2026-10-11
Recorded retrospectively; decided 2026-10-02 (error handling) and 2026-10-06 (OpenAPI schema).
Status: Accepted

## Context

Clients need one predictable error format: a machine-readable kind of error, a
status, a human message, and for validation errors, which fields were wrong.
Without a central rule, every controller invents its own JSON, and unexpected
errors leak stack traces.

Duplicates need care too. A merchant can't be registered twice for the same
country and registration number, and a merchant can't reuse a
`merchant_reference`. Checking "does it exist?" and then inserting leaves a gap:
two requests can both pass the check before either one inserts.

## Decision

**One error format: RFC 9457 Problem Details** (`application/problem+json`),
produced by a single `ProblemDetailsExceptionHandler` in `shared`. It extends
Spring's `ResponseEntityExceptionHandler`, so framework errors such as a
malformed body or a wrong type in the path come out in the same format.

The mapping is by exception type. `shared` defines three base exceptions, and
each module extends them with its own:

| Base exception | Status | Example in a module |
| --- | --- | --- |
| `DomainValidationException` | 400 | `InvalidPaymentRequestException` |
| `DomainNotFoundException` | 404 | `MerchantNotFoundException` |
| `DomainConflictException` | 409 | `IllegalMerchantTransitionException` |

An optimistic locking failure is also a 409, and anything unexpected is a 500
with a generic message. The real exception is logged on the server, never sent
to the client. Bean Validation errors add an `errors` list with `field` and
`message` for each invalid field.

**The database decides uniqueness.** The services insert with `saveAndFlush`,
so a constraint violation surfaces inside the service method instead of at
commit. They catch `DataIntegrityViolationException` and ask
`ConstraintViolations.isViolationOf(...)` which named constraint was violated
(ADR 0003). `uq_merchants_registration` becomes
`MerchantAlreadyRegisteredException`, and
`uq_payment_requests_merchant_reference` becomes
`PaymentRequestAlreadyExistsException`, both 409. Any other integrity error is
re-thrown and ends as a 500, because it means a bug.

The helper lives in `shared`, but each module keeps its own constraint names.

**The OpenAPI documentation matches.** A hand-written `Problem` schema is
attached to every 4xx and 5xx response by one `OpenApiCustomizer`, and
`springdoc.override-with-generic-response: false` stops springdoc from adding
every handled error to every endpoint. Each endpoint documents only the errors
it can really return.

### Alternatives considered

- **A custom error JSON.** Full control, but a format every client has to learn.
  Problem Details is a standard, and Spring supports it directly.
- **`@ExceptionHandler` methods per controller.** Close to the code, but the
  mapping gets copied and starts to drift between controllers.
- **Check, then insert** (`existsBy...` before `save`). Reads naturally, but has
  the race described above. The unique constraint is the only atomic check.
- **Detect duplicates by SQL state alone** (`23505`, unique violation). It says
  that some unique constraint failed, not which one. The constraint name is
  precise.

## Consequences

- Clients handle every error the same way, by `status` and `type`.
- No stack trace or SQL detail ever reaches a client.
- Constraint names are now part of the code's contract. Renaming one in a
  migration would turn a 409 into a 500. The duplicate tests in both modules
  catch that.
- The `type` URIs (`https://merchant-payments.example/problems/...`) identify the
  kind of error but don't resolve to a page. Publishing real problem
  documentation is left for later.