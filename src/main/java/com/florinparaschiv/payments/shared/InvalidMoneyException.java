package com.florinparaschiv.payments.shared;

public class InvalidMoneyException extends DomainValidationException {

    public InvalidMoneyException(String message) {
        super(message);
    }
}