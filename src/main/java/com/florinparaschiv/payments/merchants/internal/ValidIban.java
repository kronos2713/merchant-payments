package com.florinparaschiv.payments.merchants.internal;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Target({FIELD, PARAMETER})
@Retention(RUNTIME)
@Constraint(validatedBy = IbanConstraintValidator.class)
public @interface ValidIban {

    String message() default "must be a valid IBAN";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}