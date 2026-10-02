package com.florinparaschiv.payments.merchants.internal;

import com.florinparaschiv.payments.merchants.MerchantStatus;
import com.florinparaschiv.payments.shared.DomainConflictException;

public class IllegalMerchantTransitionException extends DomainConflictException {

    public IllegalMerchantTransitionException(String action, MerchantStatus current) {
        super("Cannot " + action + " a merchant that is " + current);
    }
}