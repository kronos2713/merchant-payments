package com.florinparaschiv.payments.shared;

import org.hibernate.exception.ConstraintViolationException;

public final class ConstraintViolations {

    private ConstraintViolations() {
    }

    public static boolean isViolationOf(Throwable error, String constraintName) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current instanceof ConstraintViolationException cve
                    && constraintName.equalsIgnoreCase(cve.getConstraintName())) {
                return true;
            }
        }
        return false;
    }
}