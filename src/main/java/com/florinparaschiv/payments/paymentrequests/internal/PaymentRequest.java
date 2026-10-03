package com.florinparaschiv.payments.paymentrequests.internal;

import com.florinparaschiv.payments.shared.Money;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_requests")
public class PaymentRequest {

    static final int DESCRIPTION_MAX_LENGTH = 140;
    static final int MERCHANT_REFERENCE_MAX_LENGTH = 35;

    @Id
    private UUID id;

    @Column(name = "merchant_id", nullable = false, updatable = false)
    private UUID merchantId;

    @Embedded
    private Money amount;

    @Column(name = "description", nullable = false, length = DESCRIPTION_MAX_LENGTH)
    private String description;

    @Column(name = "merchant_reference", nullable = false, updatable = false,
            length = MERCHANT_REFERENCE_MAX_LENGTH)
    private String merchantReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    private PaymentRequestStatus status;

    @Column(name = "expires_at", nullable = false, updatable = false)
    private Instant expiresAt;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected PaymentRequest() {
        // for JPA
    }

    private PaymentRequest(UUID id, UUID merchantId, Money amount, String description,
                           String merchantReference, Instant expiresAt, Instant now) {
        this.id = id;
        this.merchantId = merchantId;
        this.amount = amount;
        this.description = description;
        this.merchantReference = merchantReference;
        this.status = PaymentRequestStatus.CREATED;
        this.expiresAt = expiresAt;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public static PaymentRequest create(UUID merchantId, Money amount, String description,
                                        String merchantReference, Instant requestedExpiresAt,
                                        ExpiryPolicy expiryPolicy, Clock clock) {
        if (merchantId == null) {
            throw new InvalidPaymentRequestException("merchantId is required");
        }
        if (amount == null) {
            throw new InvalidPaymentRequestException("amount is required");
        }
        if (amount.amountMinor() <= 0) {
            throw new InvalidPaymentRequestException("amount must be positive");
        }
        requireText(description, "description", DESCRIPTION_MAX_LENGTH);
        requireText(merchantReference, "merchantReference", MERCHANT_REFERENCE_MAX_LENGTH);

        Instant now = clock.instant();
        Instant expiresAt = expiryPolicy.resolve(requestedExpiresAt, now);

        return new PaymentRequest(UUID.randomUUID(), merchantId, amount, description,
                merchantReference, expiresAt, now);
    }

    private static void requireText(String value, String field, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new InvalidPaymentRequestException(field + " is required");
        }
        if (value.length() > maxLength) {
            throw new InvalidPaymentRequestException(
                    field + " must be at most " + maxLength + " characters");
        }
    }

    PaymentRequestStatus statusAt(Instant now) {
        if (status == PaymentRequestStatus.CREATED && !now.isBefore(expiresAt)) {
            return PaymentRequestStatus.EXPIRED;
        }
        return status;
    }

    void pay(Clock clock) {
        Instant now = clock.instant();
        switch (statusAt(now)) {
            case CREATED -> {
                this.status = PaymentRequestStatus.PAID;
                this.paidAt = now;
                this.updatedAt = now;
            }
            case PAID -> throw new PaymentRequestConflictException(
                    "payment request has already been paid");
            case CANCELLED -> throw new PaymentRequestConflictException(
                    "payment request was cancelled");
            case EXPIRED -> throw new PaymentRequestConflictException(
                    "payment request has expired");
        }
    }

    void cancel(Clock clock) {
        Instant now = clock.instant();
        switch (statusAt(now)) {
            case CREATED -> {
                this.status = PaymentRequestStatus.CANCELLED;
                this.updatedAt = now;
            }
            case CANCELLED -> {
                // already in the target state: idempotent no-op, nothing changes
            }
            case PAID -> throw new PaymentRequestConflictException(
                    "payment request has been paid and cannot be cancelled");
            case EXPIRED -> throw new PaymentRequestConflictException(
                    "payment request expired at " + expiresAt);
        }
    }

    public UUID getId() { return id; }
    public UUID getMerchantId() { return merchantId; }
    public Money getAmount() { return amount; }
    public String getDescription() { return description; }
    public String getMerchantReference() { return merchantReference; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getPaidAt() { return paidAt; }
    public Long getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PaymentRequest other)) {
            return false;
        }
        return id.equals(other.getId());
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}