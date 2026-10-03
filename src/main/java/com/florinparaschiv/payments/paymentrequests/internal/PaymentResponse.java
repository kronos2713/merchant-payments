package com.florinparaschiv.payments.paymentrequests.internal;

import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        long amountMinor,
        String currency,
        String description,
        PaymentRequestStatus status,
        Instant paidAt) {

    static PaymentResponse from(PaymentRequest request, Instant now) {
        return new PaymentResponse(
                request.getId(),
                request.getAmount().amountMinor(),
                request.getAmount().currency().getCurrencyCode(),
                request.getDescription(),
                request.statusAt(now),
                request.getPaidAt());
    }
}