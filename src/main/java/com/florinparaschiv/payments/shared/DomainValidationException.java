package com.florinparaschiv.payments.shared;

public abstract class DomainValidationException extends RuntimeException {

    protected DomainValidationException(String message) {
        super(message);
    }
}