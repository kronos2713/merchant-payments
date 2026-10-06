package com.florinparaschiv.payments.paymentrequests.internal;

import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.util.UUID;

final class PaymentRequestSpecifications {

    private PaymentRequestSpecifications() {
    }

    static Specification<PaymentRequest> belongsTo(UUID merchantId) {
        return (root, query, cb) -> cb.equal(root.get("merchantId"), merchantId);
    }

    static Specification<PaymentRequest> hasMerchantReference(String merchantReference) {
        return (root, query, cb) -> cb.equal(root.get("merchantReference"), merchantReference);
    }

    static Specification<PaymentRequest> hasLogicalStatus(PaymentRequestStatus status, Instant now) {
        return (root, query, cb) -> {
            var storedStatus = root.<PaymentRequestStatus>get("status");
            var expiresAt = root.<Instant>get("expiresAt");
            return switch (status) {
                case CREATED -> cb.and(
                        cb.equal(storedStatus, PaymentRequestStatus.CREATED),
                        cb.greaterThan(expiresAt, now));
                case EXPIRED -> cb.or(
                        cb.equal(storedStatus, PaymentRequestStatus.EXPIRED),
                        cb.and(
                                cb.equal(storedStatus, PaymentRequestStatus.CREATED),
                                cb.lessThanOrEqualTo(expiresAt, now)));
                case PAID, CANCELLED -> cb.equal(storedStatus, status);
            };
        };
    }
}