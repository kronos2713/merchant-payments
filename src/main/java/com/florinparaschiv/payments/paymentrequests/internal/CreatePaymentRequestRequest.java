package com.florinparaschiv.payments.paymentrequests.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreatePaymentRequestRequest(
        @NotNull @Positive Long amountMinor,
        @NotNull @Pattern(regexp = "[A-Z]{3}") String currency,
        @NotBlank @Size(max = 140) String description,
        @NotBlank @Size(max = 35) String merchantReference,
        Instant expiresAt) {
}