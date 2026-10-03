package com.florinparaschiv.payments.shared;

import org.junit.jupiter.api.Test;

import java.util.Currency;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void createsEurAmount() {
        Money money = Money.eur(1250);

        assertThat(money.amountMinor()).isEqualTo(1250);
        assertThat(money.currency()).isEqualTo(Money.EUR);
    }

    @Test
    void allowsZeroAndNegativeAmounts() {
        assertThat(Money.eur(0).amountMinor()).isZero();
        assertThat(Money.eur(-500).amountMinor()).isEqualTo(-500);
    }

    @Test
    void rejectsMissingCurrency() {
        assertThatThrownBy(() -> new Money(100, null))
                .isInstanceOf(InvalidMoneyException.class);
    }

    @Test
    void rejectsNonEurCurrency() {
        assertThatThrownBy(() -> new Money(100, Currency.getInstance("USD")))
                .isInstanceOf(InvalidMoneyException.class)
                .hasMessageContaining("USD");
    }

    @Test
    void equalByValue() {
        assertThat(Money.eur(100)).isEqualTo(Money.eur(100));
    }
}