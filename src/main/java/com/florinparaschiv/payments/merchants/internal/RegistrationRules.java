package com.florinparaschiv.payments.merchants.internal;

import java.util.Map;
import java.util.regex.Pattern;

final class RegistrationRules {

    private static final Map<String, Pattern> NUMBER_FORMAT_BY_COUNTRY = Map.of(
            "NL", Pattern.compile("[0-9]{8}")
    );

    private RegistrationRules() {
    }

    static boolean isSupportedCountry(String country) {
        return NUMBER_FORMAT_BY_COUNTRY.containsKey(country);
    }

    /** Expects a normalised, supported country and a normalised number. */
    static boolean isValidNumber(String country, String number) {
        return NUMBER_FORMAT_BY_COUNTRY.get(country).matcher(number).matches();
    }
}