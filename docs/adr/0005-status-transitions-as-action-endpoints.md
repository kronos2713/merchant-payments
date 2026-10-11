# 0005. Change merchant status through action endpoints

Date: 2026-10-11
Recorded retrospectively; decided 2026-10-02.
Status: Accepted

## Context

A merchant moves through PENDING, ACTIVE and SUSPENDED. Not every move is
allowed: a PENDING merchant can't be suspended, and a SUSPENDED one can't be
"activated" as if it were new. The status also decides whether a merchant can
accept payment requests, so a wrong status has real consequences.

Clients will retry. A timeout doesn't tell the caller whether the change
happened, so sending the same request twice has to be safe.

## Decision

The client never sends a status. Each business action has its own endpoint and
its own method on the `Merchant` entity:

| Endpoint | Entity method | Allowed move |
| --- | --- | --- |
| `POST /merchants/{id}/activation` | `activate` | PENDING → ACTIVE |
| `POST /merchants/{id}/suspension` | `suspend` | ACTIVE → SUSPENDED |
| `POST /merchants/{id}/reactivation` | `reactivate` | SUSPENDED → ACTIVE |

The rules live in the entity, not in the controller or the service:

- **Already in the target state:** a no-op. The endpoint returns 200 with the
  merchant; `updated_at` and the version don't change.
- **Any other move:** `IllegalMerchantTransitionException`, a
  `DomainConflictException`, which becomes a 409 Problem Details response.
- **Every endpoint returns 200 with the updated merchant,** also on a no-op, so
  the client always sees the resulting state.

Activation and reactivation both end in ACTIVE, but they are separate actions.
Approving a new merchant and lifting a suspension mean different things to the
business, and later they will need different checks and a different audit
record.

The payment requests module follows the same pattern:
`POST .../{id}/cancellation` and `POST /payment-requests/{id}/payment`.

### Alternatives considered

- **`PATCH /merchants/{id}` with `{"status": "ACTIVE"}`.** Common, but the
  client then decides the state and the server has to work out which business
  action was meant. "Set ACTIVE" doesn't say whether it is a first activation or
  a reactivation.
- **One generic endpoint, `POST /merchants/{id}/transitions` with an action
  name.** Fewer endpoints, but every action shares one request type, and the
  API documentation can't describe each action separately.
- **Return 409 on a repeated call.** Strict, but a client that retries after a
  timeout would get an error for a change that actually succeeded.
- **Return 204 No Content.** Smaller responses, but the client would need a
  second call to see the result.

## Consequences

- The API reads like the business: each endpoint is one decision someone makes
  about a merchant.
- Retries are safe for every transition.
- Each new transition needs a new endpoint, entity method and test. With three
  transitions, that is cheap.
- There is no history yet: nothing records who changed a status, when or why,
  for example the reason for a suspension. An audit log with actor and reason
  is on the post-MVP hardening list, after authentication exists.
- Without authentication, anyone can call these endpoints. Phase 3 restricts
  them to an operator role.