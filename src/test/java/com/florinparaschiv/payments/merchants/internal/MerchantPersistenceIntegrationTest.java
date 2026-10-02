package com.florinparaschiv.payments.merchants.internal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.florinparaschiv.payments.TestcontainersConfiguration;
import com.florinparaschiv.payments.merchants.MerchantDirectory;
import com.florinparaschiv.payments.merchants.MerchantStatus;
import java.time.Clock;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
class MerchantPersistenceIntegrationTest {

    @Autowired
    private MerchantRepository repository;

    @Autowired
    private MerchantDirectory directory;

    @Autowired
    private Clock clock;

    @BeforeEach
    void cleanDatabase() {
        repository.deleteAll();
    }

    @Test
    void savesMerchantAndFindsSummary() {
        Merchant merchant = Merchant.register(
                "Test BV", "NL", "12345678", "NL91ABNA0417164300", clock);

        Merchant saved = repository.saveAndFlush(merchant);

        assertThat(saved.getVersion()).isZero();
        assertThat(directory.findById(merchant.getId()))
                .hasValueSatisfying(summary -> {
                    assertThat(summary.legalName()).isEqualTo("Test BV");
                    assertThat(summary.status()).isEqualTo(MerchantStatus.PENDING);
                });
    }

    @Test
    void returnsEmptyForUnknownMerchant() {
        assertThat(directory.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void rejectsDuplicateRegistration() {
        repository.saveAndFlush(Merchant.register(
                "First BV", "NL", "12345678", "NL91ABNA0417164300", clock));

        assertThatThrownBy(() -> repository.saveAndFlush(Merchant.register(
                "Second BV", "NL", "1234 5678", "NL91ABNA0417164300", clock)))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}