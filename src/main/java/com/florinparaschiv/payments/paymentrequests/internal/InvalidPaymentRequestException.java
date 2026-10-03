package com.florinparaschiv.payments.paymentrequests.internal;

import com.florinparaschiv.payments.shared.DomainValidationException;

public class InvalidPaymentRequestException extends DomainValidationException {

    public InvalidPaymentRequestException(String message) {
        super(message);
    }
}