package com.florinparaschiv.payments.paymentrequests.internal;

import com.florinparaschiv.payments.shared.DomainNotFoundException;

import java.util.UUID;

public class PaymentRequestNotFoundException extends DomainNotFoundException {

    public PaymentRequestNotFoundException(UUID id) {
        super("payment request " + id + " not found");
    }
}