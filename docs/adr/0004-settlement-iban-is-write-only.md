# 0004. Treat the settlement IBAN as write-only

Date: 2026-10-11
Recorded retrospectively; decided 2026-10-02.
Status: Accepted

## Context

The settlement IBAN is where a merchant's money is paid out. It is the most
sensitive field in the merchants module: if it leaks, it helps fraud such as
fake "please update our bank details" requests, and for a sole trader it is
personal data. No flow in this API needs to read the full IBAN back. The
merchant already knows it, and the system only needs it for payouts.

## Decision

The IBAN is accepted when a merchant registers and is never returned in full:

- `MerchantResponse` has a `maskedSettlementIban` field, for example
  `NL91 **** **** **** 4300`. The mask always has the same shape, so it hides
  the real length as well as the digits.
- `RegisterMerchantRequest.toString()` masks it too, so a log line or an
  exception that prints the request can't leak it. Tests prove that neither
  `toString` contains the full IBAN.

It is validated in two layers with the same code. `IbanValidator` normalises the
value (removes spaces, uppercases it) and checks the shape and the mod-97 check
digits. `@ValidIban` uses it on the request, so a bad IBAN gets a 400 with a
field error. `Merchant.register(...)` uses it again as the domain guard, so no
code path can create a merchant with an invalid IBAN. The database adds a shape
check as the last line (ADR 0003).

The IBAN is not unique: companies in one group can share a treasury account.

### Alternatives considered

- **Return the full IBAN.** Convenient for a merchant checking their details,
  but then every API client, browser and log that touches a merchant response
  holds it.
- **A mask that keeps the real length.** Shows more than needed. A fixed mask
  with the country, check digits and the last four characters is enough to
  recognise the account.
- **A regex check only.** Catches the wrong shape, but not a mistyped digit.
  Mod-97 catches almost every single typo, and a wrong IBAN means money sent to
  the wrong account.
- **An IBAN library such as iban4j.** It would also check each country's exact
  length. One small class was enough for the MVP, without a dependency.

## Consequences

- A merchant or support person can only recognise the account by its last four
  characters, not read it back.
- The check is generic: it doesn't know that a Dutch IBAN has exactly 18
  characters. An IBAN with the wrong length for its country but valid check
  digits would be accepted. That is rare, and acceptable for the MVP.
- The IBAN is stored in plain text in `merchants.settlement_iban`. Encrypting it
  at rest is on the post-MVP hardening list.
- There is no endpoint to change the IBAN yet. When it comes, it needs strong
  authentication (Phase 3), because changing a payout account is a classic
  fraud target.