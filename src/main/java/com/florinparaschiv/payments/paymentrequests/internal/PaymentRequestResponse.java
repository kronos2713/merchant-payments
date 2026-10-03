package com.florinparaschiv.payments.paymentrequests.internal;

import java.time.Instant;
import java.util.UUID;

public record PaymentRequestResponse(
        UUID id,
        UUID merchantId,
        long amountMinor,
        String currency,
        String description,
        String merchantReference,
        PaymentRequestStatus status,
        Instant expiresAt,
        Instant paidAt,
        Instant createdAt,
        Instant updatedAt) {

    static PaymentRequestResponse from(PaymentRequest request, Instant now) {
        return new PaymentRequestResponse(
                request.getId(),
                request.getMerchantId(),
                request.getAmount().amountMinor(),
                request.getAmount().currency().getCurrencyCode(),
                request.getDescription(),
                request.getMerchantReference(),
                request.statusAt(now),
                request.getExpiresAt(),
                request.getPaidAt(),
                request.getCreatedAt(),
                request.getUpdatedAt());
    }
}