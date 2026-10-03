package com.florinparaschiv.payments.paymentrequests.internal;

import com.florinparaschiv.payments.shared.Money;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({MerchantPaymentRequestController.class, PaymentController.class})
class PaymentRequestControllerTest {

    private static final Instant NOW = Instant.parse("2026-10-03T10:00:00Z");
    private static final Clock FIXED = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final ExpiryPolicy POLICY =
            new ExpiryPolicy(Duration.ofMinutes(5), Duration.ofDays(30), Duration.ofHours(24));
    private static final UUID MERCHANT_ID = UUID.randomUUID();

    private static final String VALID_BODY = """
            {
              "amountMinor": 1250,
              "currency": "EUR",
              "description": "Order 1001",
              "merchantReference": "ORDER-1001"
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PaymentRequestService service;

    @MockitoBean
    private Clock clock;

    @BeforeEach
    void stubClock() {
        when(clock.instant()).thenReturn(NOW);
    }

    private static PaymentRequest request() {
        return PaymentRequest.create(MERCHANT_ID, Money.eur(1250), "Order 1001",
                "ORDER-1001", null, POLICY, FIXED);
    }

    @Test
    void createReturns201WithLocation() throws Exception {
        PaymentRequest created = request();
        when(service.create(eq(MERCHANT_ID), eq(Money.eur(1250)), eq("Order 1001"),
                eq("ORDER-1001"), isNull())).thenReturn(created);

        mockMvc.perform(post("/merchants/{merchantId}/payment-requests", MERCHANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(VALID_BODY))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/payment-requests/" + created.getId())))
                .andExpect(jsonPath("$.status").value("CREATED"))
                .andExpect(jsonPath("$.currency").value("EUR"));
    }

    @Test
    void createRejectsInvalidBody() throws Exception {
        String invalid = """
                { "currency": "EUR", "description": "", "merchantReference": "REF-1" }
                """;

        mockMvc.perform(post("/merchants/{merchantId}/payment-requests", MERCHANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalid))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void createRejectsUnknownCurrency() throws Exception {
        String unknownCurrency = VALID_BODY.replace("\"EUR\"", "\"XYZ\"");

        mockMvc.perform(post("/merchants/{merchantId}/payment-requests", MERCHANT_ID)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(unknownCurrency))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getReturnsRequest() throws Exception {
        PaymentRequest existing = request();
        when(service.get(MERCHANT_ID, existing.getId())).thenReturn(existing);

        mockMvc.perform(get("/merchants/{merchantId}/payment-requests/{id}", MERCHANT_ID, existing.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.merchantReference").value("ORDER-1001"));
    }

    @Test
    void cancelReturnsCancelledRequest() throws Exception {
        PaymentRequest cancelled = request();
        cancelled.cancel(FIXED);
        when(service.cancel(MERCHANT_ID, cancelled.getId())).thenReturn(cancelled);

        mockMvc.perform(post("/merchants/{merchantId}/payment-requests/{id}/cancellation",
                        MERCHANT_ID, cancelled.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void payReturnsPayerViewWithoutMerchantDetails() throws Exception {
        PaymentRequest paid = request();
        paid.pay(FIXED);
        when(service.pay(paid.getId())).thenReturn(paid);

        mockMvc.perform(post("/payment-requests/{id}/payment", paid.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.merchantId").doesNotExist())
                .andExpect(jsonPath("$.merchantReference").doesNotExist());
    }

    @Test
    void payConflictMapsTo409() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.pay(id)).thenThrow(new PaymentRequestConflictException("payment request has already been paid"));

        mockMvc.perform(post("/payment-requests/{id}/payment", id))
                .andExpect(status().isConflict());
    }
}