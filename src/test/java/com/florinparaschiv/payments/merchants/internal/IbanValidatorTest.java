package com.florinparaschiv.payments.merchants.internal;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class IbanValidatorTest {

    @Test
    void acceptsValidIbans() {
        assertThat(IbanValidator.isValid("NL91ABNA0417164300")).isTrue();
        assertThat(IbanValidator.isValid("DE89370400440532013000")).isTrue();
        assertThat(IbanValidator.isValid("GB82WEST12345698765432")).isTrue();
    }

    @Test
    void rejectsWrongCheckDigits() {
        assertThat(IbanValidator.isValid("NL91ABNA0417164301")).isFalse();
    }

    @Test
    void rejectsBadShape() {
        assertThat(IbanValidator.isValid("NL91 ABNA 0417 1643 00")).isFalse();
        assertThat(IbanValidator.isValid("nl91abna0417164300")).isFalse();
        assertThat(IbanValidator.isValid("NL")).isFalse();
        assertThat(IbanValidator.isValid(null)).isFalse();
    }
}