package com.florinparaschiv.payments.paymentrequests.internal;

import com.florinparaschiv.payments.shared.Money;
import com.florinparaschiv.payments.shared.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/merchants/{merchantId}/payment-requests")
@Tag(name = "Payment requests (merchant)", description = "Create, read, list and cancel a merchant's payment requests")
class MerchantPaymentRequestController {

    private final PaymentRequestService service;
    private final Clock clock;

    MerchantPaymentRequestController(PaymentRequestService service, Clock clock) {
        this.service = service;
        this.clock = clock;
    }

    @PostMapping
    @Operation(summary = "Create a payment request",
            description = "The merchant must be ACTIVE. expiresAt is optional: default 24 hours, allowed range 5 minutes to 30 days from now.")
    @ApiResponse(responseCode = "201", description = "Created; the Location header points to the new request")
    @ApiResponse(responseCode = "400", description = "Invalid body, unknown currency or expiry out of range")
    @ApiResponse(responseCode = "404", description = "Unknown merchant")
    @ApiResponse(responseCode = "409", description = "Merchant not ACTIVE, or merchant reference already used")
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

    @GetMapping
    @Operation(summary = "List payment requests",
            description = "Newest first. Status is the logical status: a CREATED request past its expiresAt is reported as EXPIRED.")
    @ApiResponse(responseCode = "200", description = "A page of payment requests")
    @ApiResponse(responseCode = "400", description = "Unknown status value, page below 0, or size outside 1 to 100")
    @ApiResponse(responseCode = "404", description = "Unknown merchant")
    PageResponse<PaymentRequestResponse> list(
            @PathVariable UUID merchantId,
            @Parameter(description = "Filter by logical status")
            @RequestParam(required = false) PaymentRequestStatus status,
            @Parameter(description = "Find a request by the merchant's own reference")
            @RequestParam(required = false) String merchantReference,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size, 1 to 100")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        Instant now = clock.instant();
        Page<PaymentRequest> result = service.list(merchantId, status, merchantReference, page, size, now);
        return PageResponse.from(result.map(request -> PaymentRequestResponse.from(request, now)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a payment request")
    @ApiResponse(responseCode = "200", description = "The payment request")
    @ApiResponse(responseCode = "404", description = "Not found, or belongs to another merchant")
    PaymentRequestResponse get(@PathVariable UUID merchantId, @PathVariable UUID id) {
        return PaymentRequestResponse.from(service.get(merchantId, id), clock.instant());
    }

    @PostMapping("/{id}/cancellation")
    @Operation(summary = "Cancel a payment request",
            description = "Cancelling an already cancelled request is a no-op and returns 200.")
    @ApiResponse(responseCode = "200", description = "The cancelled payment request")
    @ApiResponse(responseCode = "404", description = "Not found, or belongs to another merchant")
    @ApiResponse(responseCode = "409", description = "Already paid, or expired")
    PaymentRequestResponse cancel(@PathVariable UUID merchantId, @PathVariable UUID id) {
        return PaymentRequestResponse.from(service.cancel(merchantId, id), clock.instant());
    }
}