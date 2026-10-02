package com.florinparaschiv.payments.merchants.internal;

import com.florinparaschiv.payments.shared.DomainNotFoundException;
import java.util.UUID;

public class MerchantNotFoundException extends DomainNotFoundException {

    public MerchantNotFoundException(UUID merchantId) {
        super("Merchant " + merchantId + " not found");
    }
}