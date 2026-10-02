package com.florinparaschiv.payments.merchants.internal;

import com.florinparaschiv.payments.shared.PageResponse;
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
class MerchantController {

    private final MerchantService service;

    MerchantController(MerchantService service) {
        this.service = service;
    }

    @PostMapping
    ResponseEntity<MerchantResponse> register(@Valid @RequestBody RegisterMerchantRequest request,
                                              UriComponentsBuilder uriBuilder) {
        MerchantResponse created = service.register(request);
        URI location = uriBuilder.path("/merchants/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/{merchantId}")
    MerchantResponse get(@PathVariable UUID merchantId) {
        return service.get(merchantId);
    }

    @GetMapping
    PageResponse<MerchantResponse> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return PageResponse.from(service.list(page, size));
    }

    @PostMapping("/{merchantId}/activation")
    MerchantResponse activate(@PathVariable UUID merchantId) {
        return service.activate(merchantId);
    }

    @PostMapping("/{merchantId}/suspension")
    MerchantResponse suspend(@PathVariable UUID merchantId) {
        return service.suspend(merchantId);
    }

    @PostMapping("/{merchantId}/reactivation")
    MerchantResponse reactivate(@PathVariable UUID merchantId) {
        return service.reactivate(merchantId);
    }
}