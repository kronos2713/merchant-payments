package com.florinparaschiv.payments.merchants.internal;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.florinparaschiv.payments.merchants.MerchantStatus;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(MerchantController.class)
class MerchantControllerTest {

    private static final UUID ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MerchantService service;

    private MerchantResponse response(MerchantStatus status) {
        Instant now = Instant.parse("2026-10-01T10:00:00Z");
        return new MerchantResponse(ID, "Test BV", "NL", "12345678",
                "NL91 **** **** **** 4300", status, now, now);
    }

    @Test
    void registersMerchant() throws Exception {
        when(service.register(any())).thenReturn(response(MerchantStatus.PENDING));

        mockMvc.perform(post("/merchants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"legalName":"Test BV","registrationCountry":"NL",
                                 "registrationNumber":"12345678",
                                 "settlementIban":"NL91 ABNA 0417 1643 00"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/merchants/" + ID)))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.maskedSettlementIban").value("NL91 **** **** **** 4300"));
    }

    @Test
    void rejectsInvalidBodyBeforeReachingService() throws Exception {
        mockMvc.perform(post("/merchants")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"legalName":"","registrationCountry":"NL",
                                 "registrationNumber":"12345678",
                                 "settlementIban":"NL91ABNA0417164301"}
                                """))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }

    @Test
    void activatesMerchant() throws Exception {
        when(service.activate(ID)).thenReturn(response(MerchantStatus.ACTIVE));

        mockMvc.perform(post("/merchants/{id}/activation", ID))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void rejectsOversizedPage() throws Exception {
        mockMvc.perform(get("/merchants").param("size", "500"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(service);
    }
}