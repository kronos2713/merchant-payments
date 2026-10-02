package com.florinparaschiv.payments.merchants.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.florinparaschiv.payments.TestcontainersConfiguration;
import com.florinparaschiv.payments.merchants.MerchantStatus;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class MerchantServiceIntegrationTest {

    @Autowired
    private MerchantService service;

    @Autowired
    private MerchantRepository repository;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    private RegisterMerchantRequest validRequest() {
        return new RegisterMerchantRequest(
                "Test BV", "NL", "12345678", "NL91 ABNA 0417 1643 00");
    }

    @Test
    void registersAndReadsBack() {
        MerchantResponse created = service.register(validRequest());

        MerchantResponse loaded = service.get(created.id());

        assertThat(loaded.status()).isEqualTo(MerchantStatus.PENDING);
        assertThat(loaded.maskedSettlementIban()).isEqualTo("NL91 **** **** **** 4300");
    }

    @Test
    void activationIsPersistedWithoutExplicitSave() {
        MerchantResponse created = service.register(validRequest());

        service.activate(created.id());

        assertThat(service.get(created.id()).status()).isEqualTo(MerchantStatus.ACTIVE);
        assertThat(repository.findById(created.id()).orElseThrow().getVersion()).isEqualTo(1L);
    }

    @Test
    void noOpActivationDoesNotBumpVersion() {
        MerchantResponse created = service.register(validRequest());
        service.activate(created.id());

        service.activate(created.id());

        assertThat(repository.findById(created.id()).orElseThrow().getVersion()).isEqualTo(1L);
    }

    @Test
    void unknownMerchantIsNotFound() {
        assertThatThrownBy(() -> service.activate(UUID.randomUUID()))
                .isInstanceOf(MerchantNotFoundException.class);
    }

    @Test
    void listsMerchants() {
        service.register(validRequest());
        service.register(new RegisterMerchantRequest(
                "Other BV", "NL", "87654321", "NL91ABNA0417164300"));

        assertThat(service.list(0, 20).getTotalElements()).isEqualTo(2);
    }
}