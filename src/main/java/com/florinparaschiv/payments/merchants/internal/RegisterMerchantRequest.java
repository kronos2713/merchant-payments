package com.florinparaschiv.payments.merchants.internal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record RegisterMerchantRequest(
        @NotBlank @Size(max = 256) String legalName,
        @NotBlank @Size(max = 2) String registrationCountry,
        @NotBlank @Size(max = 64) String registrationNumber,
        @NotBlank @ValidIban String settlementIban) {
}