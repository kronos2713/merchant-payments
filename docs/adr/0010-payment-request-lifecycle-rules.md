# 0010. Define the payment request lifecycle rules

Date: 2026-10-11
Recorded retrospectively; decided 2026-10-03.
Status: Accepted

## Context

A payment request is CREATED, then PAID, CANCELLED or EXPIRED. Three parties
touch it: the merchant creates and cancels it, the payer pays it, and an
operator can suspend the merchant behind it. Most of the risk sits in the
repeated and late cases: a second pay call, a cancel after payment, a payment
while the merchant is suspended. Each of those needed an explicit answer.

## Decision

**Creating** needs an ACTIVE merchant. An unknown merchant is a 404; a merchant
that is PENDING or SUSPENDED gets a 409 ("merchant cannot accept payment
requests"). A reused `merchant_reference` is a 409 (ADR 0006).

**Paying** (`POST /payment-requests/{id}/payment`):

| State at `now` | Result |
| --- | --- |
| CREATED, merchant ACTIVE | 200, becomes PAID with `paid_at` set |
| CREATED, merchant not ACTIVE | 409, generic message: "cannot be paid at the moment" |
| PAID | 409, "already been paid" |
| CANCELLED | 409 |
| EXPIRED | 409, nothing written (ADR 0009) |

**Cancelling** (`POST /merchants/{merchantId}/payment-requests/{id}/cancellation`):

| State at `now` | Result |
| --- | --- |
| CREATED | 200, becomes CANCELLED |
| CANCELLED | 200, no-op: nothing changes |
| PAID | 409: a paid request is refunded, not cancelled, and refunds are a separate flow |
| EXPIRED | 409 |

The same rule as for merchants (ADR 0005): repeating the action that already
happened is a no-op, anything else that doesn't fit is a 409. Paying twice is
the exception, and it is a 409 on purpose: a second pay call could be the same
payer retrying, or a different payment. Without an idempotency key, the API
can't tell which, and charging twice is worse than refusing.

**A suspended merchant's requests can't be paid,** but they stay CREATED. If the
merchant is reactivated before `expires_at`, they become payable again. There is
no automatic cancellation on suspension. The payer gets a generic message, so
the reason for the suspension isn't revealed to them.

**Only the merchant cancels.** A payer who doesn't want to pay simply lets the
request expire. Cancelling by an operator needs roles, so it waits for Phase 3.
The cancel route is nested under the merchant and checks that the request
belongs to that merchant (ADR 0011).

All transitions are in the entity, and `@Version` turns two concurrent changes
to the same request into a 409 instead of a lost update.

### Alternatives considered

- **Paying twice returns 200 with the paid request.** Friendly for retries, but
  it would also hide a real double payment attempt. That needs an idempotency
  key first.
- **Cancel a paid request means refund it.** Mixes two different money flows in
  one endpoint.
- **Cancel all open requests when a merchant is suspended.** Irreversible: a
  suspension that is lifted the same day would still have destroyed every
  open request.
- **Tell the payer that the merchant is suspended.** Honest, but it exposes the
  merchant's internal status to strangers.

## Consequences

- Every combination of state and action has one defined answer and a test.
- A payer who retries after a timeout gets a 409 even if the first call
  succeeded. Idempotency keys for payments are on the post-MVP hardening list.
- The merchant status is checked when paying, but a suspension that commits a
  moment later doesn't stop that payment. This race is accepted for the MVP and
  on the hardening list.
- The "only the merchant cancels" rule can't be enforced until authentication
  exists: today the merchant id in the path is trusted.