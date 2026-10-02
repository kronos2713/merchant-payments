package com.florinparaschiv.payments.shared;

public abstract class DomainConflictException extends RuntimeException {

    protected DomainConflictException(String message) {
        super(message);
    }
}