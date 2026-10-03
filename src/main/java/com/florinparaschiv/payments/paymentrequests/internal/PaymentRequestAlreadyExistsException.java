package com.florinparaschiv.payments.paymentrequests.internal;

import com.florinparaschiv.payments.shared.DomainConflictException;

public class PaymentRequestAlreadyExistsException extends DomainConflictException {

    public PaymentRequestAlreadyExistsException(String merchantReference) {
        super("a payment request with merchant reference '" + merchantReference + "' already exists");
    }
}