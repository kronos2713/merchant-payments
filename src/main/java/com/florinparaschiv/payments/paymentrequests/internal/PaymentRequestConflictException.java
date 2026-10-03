package com.florinparaschiv.payments.paymentrequests.internal;

import com.florinparaschiv.payments.shared.DomainConflictException;

public class PaymentRequestConflictException extends DomainConflictException {

    public PaymentRequestConflictException(String message) {
        super(message);
    }
}