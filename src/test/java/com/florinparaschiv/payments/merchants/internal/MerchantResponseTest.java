package com.florinparaschiv.payments.merchants.internal;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class MerchantResponseTest {

    private final Clock clock =
            Clock.fixed(Instant.parse("2026-10-01T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void masksSettlementIban() {
        Merchant merchant = Merchant.register(
                "Test BV", "NL", "12345678", "NL91ABNA0417164300", clock);

        MerchantResponse response = MerchantResponse.from(merchant);

        assertThat(response.maskedSettlementIban()).isEqualTo("NL91 **** **** **** 4300");
        assertThat(response.toString()).doesNotContain("NL91ABNA0417164300");
    }

    @Test
    void maskHidesLengthDifferences() {
        assertThat(MerchantResponse.mask("NL91ABNA0417164300"))
                .isEqualTo("NL91 **** **** **** 4300");
        assertThat(MerchantResponse.mask("DE89370400440532013000"))
                .isEqualTo("DE89 **** **** **** 3000");
    }
}