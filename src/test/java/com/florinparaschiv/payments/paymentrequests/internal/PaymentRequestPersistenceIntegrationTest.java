package com.florinparaschiv.payments.paymentrequests.internal;

import com.florinparaschiv.payments.TestcontainersConfiguration;
import com.florinparaschiv.payments.shared.Money;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class PaymentRequestPersistenceIntegrationTest {

    @Autowired
    private PaymentRequestRepository repository;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ExpiryPolicy expiryPolicy;

    @Autowired
    private Clock clock;

    @Test
    void expiryPolicyIsBoundFromConfiguration() {
        assertThat(expiryPolicy.min()).isEqualTo(Duration.ofMinutes(5));
        assertThat(expiryPolicy.max()).isEqualTo(Duration.ofDays(30));
        assertThat(expiryPolicy.defaultTtl()).isEqualTo(Duration.ofHours(24));
    }

    @Test
    void roundTripsThroughPostgres() {
        UUID merchantId = insertMerchant();
        PaymentRequest saved = repository.saveAndFlush(newRequest(merchantId, "ORDER-1001"));
        entityManager.clear();

        PaymentRequest loaded = repository.findById(saved.getId()).orElseThrow();

        assertThat(loaded.getMerchantId()).isEqualTo(merchantId);
        assertThat(loaded.getAmount()).isEqualTo(Money.eur(1250));
        assertThat(loaded.getDescription()).isEqualTo("Order 1001");
        assertThat(loaded.getMerchantReference()).isEqualTo("ORDER-1001");
        assertThat(loaded.statusAt(clock.instant())).isEqualTo(PaymentRequestStatus.CREATED);
        assertThat(loaded.getExpiresAt()).isEqualTo(saved.getExpiresAt());
        assertThat(loaded.getCreatedAt()).isEqualTo(saved.getCreatedAt());
        assertThat(loaded.getPaidAt()).isNull();
        assertThat(loaded.getVersion()).isZero();
    }

    @Test
    void rejectsDuplicateReferenceForSameMerchant() {
        UUID merchantId = insertMerchant();
        repository.saveAndFlush(newRequest(merchantId, "ORDER-1001"));

        assertThatThrownBy(() -> repository.saveAndFlush(newRequest(merchantId, "ORDER-1001")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void allowsSameReferenceForDifferentMerchants() {
        repository.saveAndFlush(newRequest(insertMerchant(), "ORDER-1001"));
        repository.saveAndFlush(newRequest(insertMerchant(), "ORDER-1001"));

        assertThat(repository.count()).isGreaterThanOrEqualTo(2);
    }

    @Test
    void rejectsUnknownMerchantThroughForeignKey() {
        assertThatThrownBy(() -> repository.saveAndFlush(newRequest(UUID.randomUUID(), "ORDER-1001")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    private PaymentRequest newRequest(UUID merchantId, String reference) {
        return PaymentRequest.create(merchantId, Money.eur(1250), "Order 1001",
                reference, null, expiryPolicy, clock);
    }

    private UUID insertMerchant() {
        UUID id = UUID.randomUUID();
        String registrationNumber =
                String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
        jdbcTemplate.update("""
                insert into merchants (id, legal_name, registration_country, registration_number,
                                       settlement_iban, status, version, created_at, updated_at)
                values (?, 'Test Shop B.V.', 'NL', ?, 'NL91ABNA0417164300', 'ACTIVE', 0, now(), now())
                """, id, registrationNumber);
        return id;
    }
}