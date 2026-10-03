package com.florinparaschiv.payments.paymentrequests.internal;

import com.florinparaschiv.payments.shared.Money;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Clock;
import java.util.UUID;

@RestController
@RequestMapping("/merchants/{merchantId}/payment-requests")
class MerchantPaymentRequestController {

    private final PaymentRequestService service;
    private final Clock clock;

    MerchantPaymentRequestController(PaymentRequestService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @PostMapping
    ResponseEntity<PaymentRequestResponse> create(@PathVariable UUID merchantId,
                                                  @Valid @RequestBody CreatePaymentRequestRequest body,
                                                  UriComponentsBuilder uriBuilder) {
        PaymentRequest created = service.create(merchantId,
                Money.of(body.amountMinor(), body.currency()),
                body.description(), body.merchantReference(), body.expiresAt());
        URI location = uriBuilder.path("/merchants/{merchantId}/payment-requests/{id}")
                .buildAndExpand(merchantId, created.getId())
                .toUri();
        return ResponseEntity.created(location)
                .body(PaymentRequestResponse.from(created, clock.instant()));
    }

    @GetMapping("/{id}")
    PaymentRequestResponse get(@PathVariable UUID merchantId, @PathVariable UUID id) {
        return PaymentRequestResponse.from(service.get(merchantId, id), clock.instant());
    }

    @PostMapping("/{id}/cancellation")
    PaymentRequestResponse cancel(@PathVariable UUID merchantId, @PathVariable UUID id) {
        return PaymentRequestResponse.from(service.cancel(merchantId, id), clock.instant());
    }
}