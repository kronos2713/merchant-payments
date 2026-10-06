package com.florinparaschiv.payments.merchants.internal;

import com.florinparaschiv.payments.shared.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

@RestController
@RequestMapping("/merchants")
@Tag(name = "Merchants", description = "Onboard merchants and manage their status")
class MerchantController {

    private final MerchantService service;

    MerchantController(MerchantService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Register a merchant", description = "New merchants start as PENDING.")
    @ApiResponse(responseCode = "201", description = "Registered; the Location header points to the merchant")
    @ApiResponse(responseCode = "400", description = "Invalid body, IBAN or registration number")
    @ApiResponse(responseCode = "409", description = "Registration number already registered in that country")
    ResponseEntity<MerchantResponse> register(@Valid @RequestBody RegisterMerchantRequest request,
                                              UriComponentsBuilder uriBuilder) {
        MerchantResponse created = service.register(request);
        URI location = uriBuilder.path("/merchants/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{merchantId}")
    @Operation(summary = "Get a merchant", description = "The settlement IBAN is masked.")
    @ApiResponse(responseCode = "200", description = "The merchant")
    @ApiResponse(responseCode = "404", description = "Unknown merchant")
    MerchantResponse get(@PathVariable UUID merchantId) {
        return service.get(merchantId);
    }

    @GetMapping
    @Operation(summary = "List merchants", description = "Newest first.")
    @ApiResponse(responseCode = "200", description = "A page of merchants")
    @ApiResponse(responseCode = "400", description = "page below 0, or size outside 1 to 100")
    PageResponse<MerchantResponse> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Page size, 1 to 100")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return PageResponse.from(service.list(page, size));
    }

    @PostMapping("/{merchantId}/activation")
    @Operation(summary = "Activate a merchant",
            description = "PENDING to ACTIVE. Already ACTIVE is a no-op and returns 200.")
    @ApiResponse(responseCode = "200", description = "The merchant after the transition")
    @ApiResponse(responseCode = "404", description = "Unknown merchant")
    @ApiResponse(responseCode = "409", description = "Not allowed from the current status (for example SUSPENDED: use reactivation)")
    MerchantResponse activate(@PathVariable UUID merchantId) {
        return service.activate(merchantId);
    }

    @PostMapping("/{merchantId}/suspension")
    @Operation(summary = "Suspend a merchant",
            description = "ACTIVE to SUSPENDED. Already SUSPENDED is a no-op and returns 200. Open payment requests cannot be paid while suspended.")
    @ApiResponse(responseCode = "200", description = "The merchant after the transition")
    @ApiResponse(responseCode = "404", description = "Unknown merchant")
    @ApiResponse(responseCode = "409", description = "Not allowed from the current status (for example PENDING)")
    MerchantResponse suspend(@PathVariable UUID merchantId) {
        return service.suspend(merchantId);
    }

    @PostMapping("/{merchantId}/reactivation")
    @Operation(summary = "Reactivate a merchant",
            description = "SUSPENDED to ACTIVE. Already ACTIVE is a no-op and returns 200.")
    @ApiResponse(responseCode = "200", description = "The merchant after the transition")
    @ApiResponse(responseCode = "404", description = "Unknown merchant")
    @ApiResponse(responseCode = "409", description = "Not allowed from the current status (for example PENDING: use activation)")
    MerchantResponse reactivate(@PathVariable UUID merchantId) {
        return service.reactivate(merchantId);
    }
}