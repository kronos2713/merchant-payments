package com.florinparaschiv.payments.paymentrequests.internal;

import com.florinparaschiv.payments.shared.Money;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentRequestTest {

    private static final Instant NOW = Instant.parse("2026-10-03T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final ExpiryPolicy POLICY =
            new ExpiryPolicy(Duration.ofMinutes(5), Duration.ofDays(30), Duration.ofHours(24));
    private static final UUID MERCHANT_ID = UUID.randomUUID();

    private static PaymentRequest create(long amountMinor, String reference, Instant expiresAt) {
        return PaymentRequest.create(MERCHANT_ID, Money.eur(amountMinor), "Order 1001",
                reference, expiresAt, POLICY, CLOCK);
    }

    @Test
    void createsWithDefaultsWhenExpiryOmitted() {
        PaymentRequest request = create(1250, "ORDER-1001", null);

        assertThat(request.getId()).isNotNull();
        assertThat(request.statusAt(NOW)).isEqualTo(PaymentRequestStatus.CREATED);
        assertThat(request.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofHours(24)));
        assertThat(request.getCreatedAt()).isEqualTo(NOW);
        assertThat(request.getUpdatedAt()).isEqualTo(NOW);
        assertThat(request.getPaidAt()).isNull();
    }

    @Test
    void acceptsExpiryExactlyAtMinimum() {
        Instant atMin = NOW.plus(Duration.ofMinutes(5));

        assertThat(create(100, "REF-1", atMin).getExpiresAt()).isEqualTo(atMin);
    }

    @Test
    void rejectsExpiryBelowMinimum() {
        Instant tooSoon = NOW.plus(Duration.ofMinutes(4));

        assertThatThrownBy(() -> create(100, "REF-1", tooSoon))
                .isInstanceOf(InvalidPaymentRequestException.class);
    }

    @Test
    void rejectsExpiryAboveMaximum() {
        Instant tooLate = NOW.plus(Duration.ofDays(31));

        assertThatThrownBy(() -> create(100, "REF-1", tooLate))
                .isInstanceOf(InvalidPaymentRequestException.class);
    }

    @Test
    void truncatesRequestedExpiryToMicroseconds() {
        Instant withNanos = NOW.plus(Duration.ofHours(1)).plusNanos(123_456_789);

        assertThat(create(100, "REF-1", withNanos).getExpiresAt().getNano() % 1_000).isZero();
    }

    @Test
    void rejectsNonPositiveAmount() {
        assertThatThrownBy(() -> create(0, "REF-1", null))
                .isInstanceOf(InvalidPaymentRequestException.class)
                .hasMessageContaining("positive");
    }

    @Test
    void rejectsBlankReference() {
        assertThatThrownBy(() -> create(100, "  ", null))
                .isInstanceOf(InvalidPaymentRequestException.class);
    }

    @Test
    void rejectsReferenceLongerThan35() {
        assertThatThrownBy(() -> create(100, "R".repeat(36), null))
                .isInstanceOf(InvalidPaymentRequestException.class);
    }

    @Test
    void reportsExpiredFromExpiresAtOnwards() {
        PaymentRequest request = create(100, "REF-1", null);
        Instant expiresAt = request.getExpiresAt();

        assertThat(request.statusAt(expiresAt.minusNanos(1_000))).isEqualTo(PaymentRequestStatus.CREATED);
        assertThat(request.statusAt(expiresAt)).isEqualTo(PaymentRequestStatus.EXPIRED);
    }

    @Test
    void rejectsInconsistentPolicy() {
        assertThatThrownBy(() -> new ExpiryPolicy(Duration.ofDays(1), Duration.ofHours(1), Duration.ofHours(2)))
                .isInstanceOf(IllegalArgumentException.class);
    }
    private static Clock at(Instant instant) {
        return Clock.fixed(instant, ZoneOffset.UTC);
    }

    @Test
    void paysOpenRequest() {
        PaymentRequest request = create(100, "REF-1", null);
        Instant payTime = NOW.plus(Duration.ofHours(1));

        request.pay(at(payTime));

        assertThat(request.statusAt(payTime)).isEqualTo(PaymentRequestStatus.PAID);
        assertThat(request.getPaidAt()).isEqualTo(payTime);
        assertThat(request.getUpdatedAt()).isEqualTo(payTime);
    }

    @Test
    void paidRequestStaysPaidAfterExpiryTime() {
        PaymentRequest request = create(100, "REF-1", null);
        request.pay(at(NOW.plus(Duration.ofHours(1))));

        assertThat(request.statusAt(request.getExpiresAt().plus(Duration.ofDays(1))))
                .isEqualTo(PaymentRequestStatus.PAID);
    }

    @Test
    void rejectsPayingTwice() {
        PaymentRequest request = create(100, "REF-1", null);
        request.pay(at(NOW.plus(Duration.ofHours(1))));

        assertThatThrownBy(() -> request.pay(at(NOW.plus(Duration.ofHours(2)))))
                .isInstanceOf(PaymentRequestConflictException.class);
    }

    @Test
    void rejectsPayingExpiredRequestWithoutChangingIt() {
        PaymentRequest request = create(100, "REF-1", null);

        assertThatThrownBy(() -> request.pay(at(request.getExpiresAt())))
                .isInstanceOf(PaymentRequestConflictException.class);
        assertThat(request.getPaidAt()).isNull();
        assertThat(request.getUpdatedAt()).isEqualTo(NOW);
    }

    @Test
    void rejectsPayingCancelledRequest() {
        PaymentRequest request = create(100, "REF-1", null);
        request.cancel(at(NOW.plus(Duration.ofHours(1))));

        assertThatThrownBy(() -> request.pay(at(NOW.plus(Duration.ofHours(2)))))
                .isInstanceOf(PaymentRequestConflictException.class);
    }

    @Test
    void cancelsOpenRequest() {
        PaymentRequest request = create(100, "REF-1", null);
        Instant cancelTime = NOW.plus(Duration.ofHours(1));

        request.cancel(at(cancelTime));

        assertThat(request.statusAt(cancelTime)).isEqualTo(PaymentRequestStatus.CANCELLED);
        assertThat(request.getUpdatedAt()).isEqualTo(cancelTime);
    }

    @Test
    void cancellingTwiceIsNoOp() {
        PaymentRequest request = create(100, "REF-1", null);
        Instant firstCancel = NOW.plus(Duration.ofHours(1));
        request.cancel(at(firstCancel));

        request.cancel(at(NOW.plus(Duration.ofHours(2))));

        assertThat(request.getUpdatedAt()).isEqualTo(firstCancel);
    }

    @Test
    void rejectsCancellingPaidRequest() {
        PaymentRequest request = create(100, "REF-1", null);
        request.pay(at(NOW.plus(Duration.ofHours(1))));

        assertThatThrownBy(() -> request.cancel(at(NOW.plus(Duration.ofHours(2)))))
                .isInstanceOf(PaymentRequestConflictException.class);
    }

    @Test
    void rejectsCancellingExpiredRequestWithoutChangingIt() {
        PaymentRequest request = create(100, "REF-1", null);

        assertThatThrownBy(() -> request.cancel(at(request.getExpiresAt())))
                .isInstanceOf(PaymentRequestConflictException.class);
        assertThat(request.getUpdatedAt()).isEqualTo(NOW);
    }
}