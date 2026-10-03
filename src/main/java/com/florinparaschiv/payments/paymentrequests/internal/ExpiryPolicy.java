package com.florinparaschiv.payments.paymentrequests.internal;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@ConfigurationProperties("payments.payment-requests.expiry")
public record ExpiryPolicy(
        @DefaultValue("5m") Duration min,
        @DefaultValue("30d") Duration max,
        @DefaultValue("24h") Duration defaultTtl) {

    public ExpiryPolicy {
        if (min == null || max == null || defaultTtl == null) {
            throw new IllegalArgumentException("expiry policy durations are required");
        }
        if (min.isNegative() || min.isZero()) {
            throw new IllegalArgumentException("expiry min must be positive");
        }
        if (max.compareTo(min) < 0) {
            throw new IllegalArgumentException("expiry max must not be below min");
        }
        if (defaultTtl.compareTo(min) < 0 || defaultTtl.compareTo(max) > 0) {
            throw new IllegalArgumentException("expiry default-ttl must be between min and max");
        }
    }

    Instant resolve(Instant requestedExpiresAt, Instant now) {
        if (requestedExpiresAt == null) {
            return now.plus(defaultTtl);
        }
        Instant expiresAt = requestedExpiresAt.truncatedTo(ChronoUnit.MICROS);
        if (expiresAt.isBefore(now.plus(min)) || expiresAt.isAfter(now.plus(max))) {
            throw new InvalidPaymentRequestException(
                    "expiresAt must be between " + min.toMinutes() + " minutes and "
                            + max.toDays() + " days from now");
        }
        return expiresAt;
    }
}