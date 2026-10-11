# 0012. Filter the payment request list by logical status

Date: 2026-10-11
Recorded retrospectively; decided 2026-10-03 (filters) and 2026-10-06 (paging validation).
Status: Accepted

## Context

A merchant needs to find its payment requests: all of them, the open ones, the
paid ones, or the one with a given `merchant_reference`. The status a merchant
cares about is the logical one (ADR 0009): a request past its `expires_at` is
EXPIRED, even though the stored status still says CREATED. A filter on the
stored column alone would list expired requests as open.

## Decision

One endpoint: `GET /merchants/{merchantId}/payment-requests`, with optional
`status`, `merchantReference`, `page` and `size` query parameters.

**`?status=` is the logical status,** translated to SQL in
`PaymentRequestSpecifications.hasLogicalStatus`:

| Filter | SQL condition |
| --- | --- |
| CREATED | `status = 'CREATED' AND expires_at > now` |
| EXPIRED | `status = 'EXPIRED' OR (status = 'CREATED' AND expires_at <= now)` |
| PAID, CANCELLED | `status = <value>` |

The EXPIRED filter also matches a stored EXPIRED, which nothing writes today.
When the scheduled sweep starts storing it, the query needs no change.

**One `now` per request.** The controller reads the clock once and passes the
same instant to the query and to the response mapping. A request that expires
during the call can't be selected as CREATED and then shown as EXPIRED in the
same response.

**`?merchantReference=` is a filter, not a separate lookup endpoint.** It goes
through the same ownership and paging rules.

**The rest:**

- The merchant filter is always applied first: `belongsTo(merchantId)`.
- An unknown merchant is a 404; a known merchant without requests gets an empty
  page.
- Sorted by `createdAt` descending, then `id` descending, so pages are stable
  when timestamps are equal.
- `page` starts at 0, `size` defaults to 20 and may be 1 to 100. Anything else,
  or an unknown status value, is a 400. This is the same rule as the merchant
  list. I first clamped invalid values silently, then changed it, because one
  rule across the API is easier to explain than two.
- The response is the shared `PageResponse`: `items`, `page`, `size`,
  `totalElements`, `totalPages`.

Optional filters are combined with Spring Data Specifications.

### Alternatives considered

- **Filter on the stored status.** Simple, and wrong: expired requests would be
  listed as open until a sweep exists.
- **A separate endpoint for the reference lookup**
  (`.../payment-requests/by-reference/{ref}`). Common, but it duplicates the
  ownership check and returns a different shape for what is really a filtered
  list.
- **One repository method per filter combination.** Readable with two filters,
  but the number of methods doubles with every new filter.
- **Clamp invalid `page` and `size`.** Friendlier, but it hides client bugs, and
  the merchant list already returned 400.

## Consequences

- The open list is correct at every moment, without a background job.
- Adding a filter is one more `Specification`, not a new repository method.
- Offset paging runs a count query for `totalElements`, and deep pages get
  slower. There is no index on `(merchant_id, created_at, id)` yet. That index,
  and cursor-based paging if merchants get many requests, are on the post-MVP
  hardening list.