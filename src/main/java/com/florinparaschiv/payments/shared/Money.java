package com.florinparaschiv.payments.shared;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.util.Currency;

@Embeddable
public record Money(
        @Column(name = "amount_minor", nullable = false) long amountMinor,
        @Column(name = "currency", nullable = false, length = 3) Currency currency) {

    public static final Currency EUR = Currency.getInstance("EUR");

    public Money {
        if (currency == null) {
            throw new InvalidMoneyException("currency is required");
        }
        if (!EUR.equals(currency)) {
            throw new InvalidMoneyException("only EUR is supported, got " + currency.getCurrencyCode());
        }
    }

    public static Money eur(long amountMinor) {
        return new Money(amountMinor, EUR);
    }

    public static Money of(long amountMinor, String currencyCode) {
        if (currencyCode == null) {
            throw new InvalidMoneyException("currency is required");
        }
        try {
            return new Money(amountMinor, Currency.getInstance(currencyCode));
        } catch (IllegalArgumentException e) {
            throw new InvalidMoneyException("unknown currency " + currencyCode);
        }
    }
}