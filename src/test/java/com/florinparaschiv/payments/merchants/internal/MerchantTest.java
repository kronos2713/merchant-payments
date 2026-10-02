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

    @Test
    void rejectsUnsupportedCountry() {
        assertThatThrownBy(() -> Merchant.register(
                "Test BV", "BE", "0123456789", "NL91ABNA0417164300", clock))
                .isInstanceOf(InvalidMerchantDataException.class)
                .hasMessageContaining("not supported");
    }

    @Test
    void rejectsWrongDutchRegistrationNumber() {
        assertThatThrownBy(() -> Merchant.register(
                "Test BV", "NL", "1234567", "NL91ABNA0417164300", clock))
                .isInstanceOf(InvalidMerchantDataException.class)
                .hasMessageContaining("registrationNumber");
    }

    private final Clock later =
            Clock.fixed(Instant.parse("2026-10-02T09:00:00Z"), ZoneOffset.UTC);

    private Merchant newMerchant() {
        return Merchant.register("Test BV", "NL", "12345678", "NL91ABNA0417164300", clock);
    }

    @Test
    void activatesPendingMerchant() {
        Merchant merchant = newMerchant();

        merchant.activate(later);

        assertThat(merchant.getStatus()).isEqualTo(MerchantStatus.ACTIVE);
        assertThat(merchant.getUpdatedAt()).isEqualTo(later.instant());
        assertThat(merchant.getCreatedAt()).isEqualTo(clock.instant());
    }

    @Test
    void activatingActiveMerchantChangesNothing() {
        Merchant merchant = newMerchant();
        merchant.activate(clock);

        merchant.activate(later);

        assertThat(merchant.getStatus()).isEqualTo(MerchantStatus.ACTIVE);
        assertThat(merchant.getUpdatedAt()).isEqualTo(clock.instant());
    }

    @Test
    void suspendsAndReactivates() {
        Merchant merchant = newMerchant();
        merchant.activate(clock);

        merchant.suspend(clock);
        assertThat(merchant.getStatus()).isEqualTo(MerchantStatus.SUSPENDED);

        merchant.reactivate(later);
        assertThat(merchant.getStatus()).isEqualTo(MerchantStatus.ACTIVE);
        assertThat(merchant.getUpdatedAt()).isEqualTo(later.instant());
    }

    @Test
    void cannotSuspendPendingMerchant() {
        Merchant merchant = newMerchant();

        assertThatThrownBy(() -> merchant.suspend(clock))
                .isInstanceOf(IllegalMerchantTransitionException.class)
                .hasMessageContaining("PENDING");
    }

    @Test
    void cannotReactivatePendingMerchant() {
        Merchant merchant = newMerchant();

        assertThatThrownBy(() -> merchant.reactivate(clock))
                .isInstanceOf(IllegalMerchantTransitionException.class);
    }

    @Test
    void cannotActivateSuspendedMerchant() {
        Merchant merchant = newMerchant();
        merchant.activate(clock);
        merchant.suspend(clock);

        assertThatThrownBy(() -> merchant.activate(clock))
                .isInstanceOf(IllegalMerchantTransitionException.class)
                .hasMessageContaining("SUSPENDED");
    }
}