package com.florinparaschiv.payments.paymentrequests.internal;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.util.UUID;

@RestController
@RequestMapping("/payment-requests")
class PaymentController {

    private final PaymentRequestService service;
    private final Clock clock;

    PaymentController(PaymentRequestService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @PostMapping("/{id}/payment")
    PaymentResponse pay(@PathVariable UUID id) {
        return PaymentResponse.from(service.pay(id), clock.instant());
    }
}