package com.florinparaschiv.payments.merchants.internal;

import com.florinparaschiv.payments.merchants.MerchantStatus;
import java.time.Instant;
import java.util.UUID;

record MerchantResponse(
        UUID id,
        String legalName,
        String registrationCountry,
        String registrationNumber,
        String maskedSettlementIban,
        MerchantStatus status,
        Instant createdAt,
        Instant updatedAt) {

    static MerchantResponse from(Merchant merchant) {
        return new MerchantResponse(
                merchant.getId(),
                merchant.getLegalName(),
                merchant.getRegistrationCountry(),
                merchant.getRegistrationNumber(),
                mask(merchant.getSettlementIban()),
                merchant.getStatus(),
                merchant.getCreatedAt(),
                merchant.getUpdatedAt());
    }

    static String mask(String iban) {
        if (iban == null || iban.length() <= 8) {
            return "****";
        }
        return iban.substring(0, 4) + " **** **** **** " + iban.substring(iban.length() - 4);
    }
}