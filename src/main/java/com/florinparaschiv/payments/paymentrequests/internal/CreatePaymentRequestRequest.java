package com.florinparaschiv.payments.paymentrequests.internal;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public record CreatePaymentRequestRequest(
        @Schema(description = "Amount in minor units (cents)", example = "1250")
        @NotNull @Positive Long amountMinor,

        @Schema(description = "ISO 4217 code; only EUR is accepted for now", example = "EUR")
        @NotNull @Pattern(regexp = "[A-Z]{3}") String currency,

        @Schema(description = "Shown to the payer", example = "Order 1001")
        @NotBlank @Size(max = 140) String description,

        @Schema(description = "The merchant's own reference, unique per merchant", example = "ORDER-1001")
        @NotBlank @Size(max = 35) String merchantReference,

        @Schema(description = "Optional; defaults to 24 hours from now", example = "2026-10-07T12:00:00Z",
                requiredMode = Schema.RequiredMode.NOT_REQUIRED)
        Instant expiresAt) {
}