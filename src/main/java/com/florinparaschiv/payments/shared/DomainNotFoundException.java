package com.florinparaschiv.payments.shared;

public abstract class DomainNotFoundException extends RuntimeException {

    protected DomainNotFoundException(String message) {
        super(message);
    }
}