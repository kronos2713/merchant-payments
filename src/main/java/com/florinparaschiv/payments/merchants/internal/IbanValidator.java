package com.florinparaschiv.payments.merchants.internal;

import java.util.Locale;

final class IbanValidator {

    private IbanValidator() {
    }

    static String normalise(String rawIban) {
        return rawIban.replaceAll("\\s", "").toUpperCase(Locale.ROOT);
    }

    /** Expects a normalised IBAN: no spaces, uppercase. */
    static boolean isValid(String iban) {
        if (iban == null || !iban.matches("[A-Z]{2}[0-9]{2}[A-Z0-9]{1,30}")) {
            return false;
        }
        String rearranged = iban.substring(4) + iban.substring(0, 4);
        int remainder = 0;
        for (char c : rearranged.toCharArray()) {
            if (Character.isDigit(c)) {
                remainder = (remainder * 10 + (c - '0')) % 97;
            } else {
                remainder = (remainder * 100 + (c - 'A' + 10)) % 97;
            }
        }
        return remainder == 1;
    }
}