package com.florinparaschiv.payments.merchants.internal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.florinparaschiv.payments.TestcontainersConfiguration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class MerchantApiErrorsIntegrationTest {

    private static final String VALID_BODY = """
            {"legalName":"Test BV","registrationCountry":"NL",
             "registrationNumber":"12345678","settlementIban":"NL91ABNA0417164300"}
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MerchantRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void duplicateRegistrationIsConflict() throws Exception {
        mockMvc.perform(post("/merchants").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/merchants").contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Conflict with current state"))
                .andExpect(jsonPath("$.detail").value("A merchant with registration NL 12345678 already exists"));
    }

    @Test
    void unknownMerchantIsNotFound() throws Exception {
        mockMvc.perform(get("/merchants/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void illegalTransitionIsConflict() throws Exception {
        String location = mockMvc.perform(post("/merchants")
                        .contentType(MediaType.APPLICATION_JSON).content(VALID_BODY))
                .andReturn().getResponse().getHeader("Location");

        mockMvc.perform(post(location + "/suspension"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Cannot suspend a merchant that is PENDING"));
    }

    @Test
    void unsupportedCountryIsBadRequest() throws Exception {
        mockMvc.perform(post("/merchants").contentType(MediaType.APPLICATION_JSON).content("""
                        {"legalName":"Test BV","registrationCountry":"BE",
                         "registrationNumber":"0123456789","settlementIban":"NL91ABNA0417164300"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("registrationCountry BE is not supported yet"));
    }

    @Test
    void invalidFieldsAreListed() throws Exception {
        mockMvc.perform(post("/merchants").contentType(MediaType.APPLICATION_JSON).content("""
                        {"legalName":"","registrationCountry":"NL",
                         "registrationNumber":"12345678","settlementIban":"NL91ABNA0417164301"}
                        """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(2));
    }

    @Test
    void nonUuidPathIsBadRequest() throws Exception {
        mockMvc.perform(get("/merchants/not-a-uuid"))
                .andExpect(status().isBadRequest());
    }
}