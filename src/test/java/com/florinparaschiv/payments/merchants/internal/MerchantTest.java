package com.florinparaschiv.payments.merchants.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.florinparaschiv.payments.merchants.MerchantStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class MerchantTest {

    private final Clock clock =
            Clock.fixed(Instant.parse("2026-10-01T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void registersNormalisedPendingMerchant() {
        Merchant merchant = Merchant.register(
                "  Test BV ", "nl", "1234 5678", "NL91 ABNA 0417 1643 00", clock);

        assertThat(merchant.getId()).isNotNull();
        assertThat(merchant.getLegalName()).isEqualTo("Test BV");
        assertThat(merchant.getRegistrationCountry()).isEqualTo("NL");
        assertThat(merchant.getRegistrationNumber()).isEqualTo("12345678");
        assertThat(merchant.getSettlementIban()).isEqualTo("NL91ABNA0417164300");
        assertThat(merchant.getStatus()).isEqualTo(MerchantStatus.PENDING);
        assertThat(merchant.getCreatedAt()).isEqualTo(Instant.parse("2026-10-01T10:00:00Z"));
        assertThat(merchant.getUpdatedAt()).isEqualTo(merchant.getCreatedAt());
    }

    @Test
    void rejectsInvalidIban() {
        assertThatThrownBy(() -> Merchant.register(
                "Test BV", "NL", "12345678", "NL91ABNA0417164301", clock))
                .isInstanceOf(InvalidMerchantDataException.class)
                .hasMessageContaining("settlementIban");
    }

    @Test
    void rejectsBlankLegalName() {
        assertThatThrownBy(() -> Merchant.register(
                "   ", "NL", "12345678", "NL91ABNA0417164300", clock))
                .isInstanceOf(InvalidMerchantDataException.class)
                .hasMessageContaining("legalName");
    }
}