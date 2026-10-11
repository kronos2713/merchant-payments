# 0011. Split the payment request routes by caller

Date: 2026-10-11
Recorded retrospectively; decided 2026-10-03.
Status: Accepted

## Context

Two different parties use payment requests. The merchant creates, reads, lists
and cancels its own requests, and must never see another merchant's. The payer
only has the link to one request and pays it. They need different data: the
merchant wants its own reference, the payer doesn't need to know anything about
the merchant's internals.

## Decision

The routes follow the caller.

**Merchant routes,** nested under the merchant:

- `POST /merchants/{merchantId}/payment-requests` (create)
- `GET /merchants/{merchantId}/payment-requests` (list, ADR 0012)
- `GET /merchants/{merchantId}/payment-requests/{id}`
- `POST /merchants/{merchantId}/payment-requests/{id}/cancellation`

Every lookup uses `findByIdAndMerchantId`. A request that belongs to another
merchant returns 404, exactly like a request that doesn't exist, so the API
doesn't confirm that someone else's id is real.

**Payer route,** flat: `POST /payment-requests/{id}/payment`. It loads by id
only, because the payer doesn't know the merchant id. Its response,
`PaymentResponse`, contains the amount, currency, description, status and
`paid_at`, and leaves out `merchantId` and `merchantReference`.

In the OpenAPI documentation the two sides are separate groups:
"Payment requests (merchant)" and "Payments (payer)".

### Alternatives considered

- **One flat resource, `/payment-requests/{id}`, for everyone.** Simpler URLs,
  but the ownership check has to come from somewhere else, and both callers get
  the same response, so the payer sees the merchant's fields.
- **Return 403 for another merchant's request.** Clearer for debugging, but it
  confirms that the id exists. 404 gives nothing away.
- **Nest the payer route under the merchant too.** Consistent, but the payer's
  link would expose the merchant id for no reason.

## Consequences

- Ownership is part of every merchant query, not a check that can be
  forgotten afterwards.
- The payer view is minimal by construction: a field has to be added to
  `PaymentResponse` on purpose before a payer can see it.
- Today the `merchantId` in the path is trusted: without authentication,
  anyone who knows a merchant id can act as that merchant. Phase 3 takes the
  merchant from the token and checks it against the path.
- The payer has no endpoint to view a request before paying it. A real payment
  page would need `GET /payment-requests/{id}` with the same minimal response.
  It isn't in the MVP.