# 0001. Build a modular monolith with Spring Modulith

Date: 2026-10-11
Status: Accepted

## Context

merchant-payments is a payment-request service shaped like iDEAL: merchants are
onboarded, create payment requests, and payers pay them. I build it alone, about
two hours a day. Creating a payment request needs to know whether the merchant
is ACTIVE, so the two parts of the domain talk to each other on every request.

I wanted clear boundaries between those parts from day one. At work I maintain
systems where every part can reach into every other part, and that is what makes
them expensive to change.

## Decision

One deployable Spring Boot application, split into three Spring Modulith modules:

- `merchants`: onboarding and the merchant lifecycle (PENDING, ACTIVE, SUSPENDED).
  Registration is EU-shaped (country plus national registration number, unique
  per pair), but only countries with a defined rule are accepted. For now that
  is NL.
- `paymentrequests`: creating, paying, cancelling and listing payment requests.
- `shared`: building blocks every module may use (`Money`, `Clock`, base
  exceptions, Problem Details, paging). It is an open module.

Each module keeps its entities, repositories and services in an `internal`
package. The only thing `paymentrequests` knows about merchants is the public
`MerchantDirectory` interface, which returns a `MerchantSummary` (id, legal name,
status). It never sees the `Merchant` entity.

The boundaries are enforced, not just agreed: `ModularityTests` runs
`ApplicationModules.verify()` on every build, so CI fails if a module reaches
into another module's internals.

I use `spring-modulith-starter-core` rather than `-starter-jpa`. The JPA starter
brings the event publication registry, which needs its own table, and Phase 1
publishes no events.

### Alternatives considered

- **Microservices.** One developer, no need to deploy or scale the parts
  separately, and the merchant check would become a network call with timeouts
  and partial failures instead of a method call inside one transaction. That is
  a lot of operational cost for no benefit at this size.
- **A plain layered monolith.** Fastest to start, but nothing stops
  `paymentrequests` from using the `Merchant` entity directly. Boundaries that
  are only conventions erode over time.

## Consequences

- One database transaction can check the merchant and create the payment request.
- A module can be extracted into its own service later: its public API is
  already small and explicit.
- Accepted coupling: `payment_requests.merchant_id` has a foreign key to
  `merchants`, which crosses the module boundary at the database level. It
  guarantees the link for the MVP; it would have to go if a module is ever
  extracted.
- Event publication between modules (registry or a hand-written outbox) is a
  Phase 2 decision.