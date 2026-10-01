package com.florinparaschiv.payments.merchants.internal;

import com.florinparaschiv.payments.shared.DomainValidationException;

public class InvalidMerchantDataException extends DomainValidationException {

    public InvalidMerchantDataException(String message) {
        super(message);
    }
}