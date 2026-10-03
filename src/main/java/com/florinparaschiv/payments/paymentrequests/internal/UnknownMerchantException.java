package com.florinparaschiv.payments.paymentrequests.internal;

import com.florinparaschiv.payments.shared.DomainNotFoundException;

import java.util.UUID;

public class UnknownMerchantException extends DomainNotFoundException {

    public UnknownMerchantException(UUID merchantId) {
        super("merchant " + merchantId + " not found");
    }
}