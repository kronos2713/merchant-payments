package com.florinparaschiv.payments.paymentrequests.internal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.util.UUID;

@RestController
@RequestMapping("/payment-requests")
@Tag(name = "Payments (payer)", description = "The payer's side: pay a payment request from its link")
class PaymentController {

    private final PaymentRequestService service;
    private final Clock clock;

    PaymentController(PaymentRequestService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @PostMapping("/{id}/payment")
    @Operation(summary = "Pay a payment request",
            description = "Phase 1 simulates the payer's bank confirming the payment. Payable strictly before expiresAt.")
    @ApiResponse(responseCode = "200", description = "Paid; payer view without merchant details")
    @ApiResponse(responseCode = "404", description = "Unknown payment request")
    @ApiResponse(responseCode = "409", description = "Already paid, cancelled, expired, or cannot be paid at the moment")
    PaymentResponse pay(@PathVariable UUID id) {
        return PaymentResponse.from(service.pay(id), clock.instant());
    }
}