package com.florinparaschiv.payments.merchants.internal;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RegisterMerchantRequestTest {

    private final Validator validator =
            Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void acceptsValidRequestWithSpacedIban() {
        var request = new RegisterMerchantRequest(
                "Test BV", "NL", "12345678", "NL91 ABNA 0417 1643 00");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void reportsEveryInvalidField() {
        var request = new RegisterMerchantRequest(
                "", "NL", "12345678", "NL91ABNA0417164301");

        Set<ConstraintViolation<RegisterMerchantRequest>> violations =
                validator.validate(request);

        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .containsExactlyInAnyOrder("legalName", "settlementIban");
    }
}