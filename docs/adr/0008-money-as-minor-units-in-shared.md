# 0008. Model money as minor units in a shared value object

Date: 2026-10-11
Recorded retrospectively; decided 2026-10-03.
Status: Accepted

## Context

A payment request has an amount and a currency. Money bugs are some of the
worst a payment system can have: a rounding error or a mixed-up currency
moves real money. Payment requests are the first module that needs money, but
settlement and fees (Phase 4) will need it too.

## Decision

Money is a `Money` record in `shared`: a `long amountMinor` (cents) and a
`java.util.Currency`.

- **Minor units in a `long`.** 12.50 EUR is stored and sent as `1250`. Integer
  arithmetic is exact, and there is no rounding mode to choose.
- **`java.util.Currency`, not a `String`.** Only real ISO 4217 codes can exist.
  `Money.of(1250, "XYZ")` throws `InvalidMoneyException`, which becomes a 400.
- **EUR only for now,** enforced in the record's constructor. The database has
  the same rule as a check constraint (ADR 0003).
- **`@Embeddable`.** It maps onto the existing `amount_minor` and `currency`
  columns, so the entity holds one `Money` instead of two loose fields.
- **Zero and negative amounts are allowed in `Money` itself.** Refunds, fees and
  settlement totals can be zero or negative. "Must be positive" is a rule of a
  payment request, so it is checked in `PaymentRequest.create(...)`.

The API uses the same shape: `amountMinor` and `currency` in the JSON, never a
decimal number.

### Alternatives considered

- **`BigDecimal`.** Exact, but every operation needs a scale and a rounding
  mode, and `1.0` and `1.00` are not `equals`. Minor units avoid both problems.
- **`double`.** Can't represent most decimal amounts exactly. Never for money.
- **Two plain fields on the entity.** Nothing keeps the amount and the currency
  together, and every module would repeat the currency checks.
- **A money library** (JSR 354 / Moneta, or Joda-Money). Complete, but much more
  than a single-currency MVP needs, and one more dependency to explain.
- **`Money` inside `paymentrequests`.** Simpler today, but settlement would then
  depend on the payment requests module just to reuse a value type.

## Consequences

- Amounts are exact everywhere: in Java, in the database and in the API.
- Clients have to send cents. The OpenAPI example shows `1250`, so it is
  visible in the documentation.
- "Minor units" assumes two decimals. That holds for EUR, but not for every
  currency: JPY has none, and some currencies have three. Supporting a second
  currency means using `Currency.getDefaultFractionDigits()`, a new migration
  for the check constraint, and changes to the constructor rule.
- `Money` has no arithmetic yet. Methods like `plus` that refuse mixed
  currencies come when settlement needs them.