package com.florinparaschiv.payments.merchants.internal;

import com.florinparaschiv.payments.shared.DomainConflictException;

public class MerchantAlreadyRegisteredException extends DomainConflictException {

    public MerchantAlreadyRegisteredException(String country, String number) {
        super("A merchant with registration " + country + " " + number + " already exists");
    }
}