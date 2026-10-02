package com.florinparaschiv.payments.merchants;

import java.util.UUID;

public record MerchantSummary(UUID id, String legalName, MerchantStatus status) {
}