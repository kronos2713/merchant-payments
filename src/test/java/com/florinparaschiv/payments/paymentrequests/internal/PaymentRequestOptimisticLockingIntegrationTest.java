package com.florinparaschiv.payments.paymentrequests.internal;

import com.florinparaschiv.payments.TestcontainersConfiguration;
import com.florinparaschiv.payments.shared.Money;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Clock;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Not @Transactional on purpose: each transaction must really commit for the race to happen.
 * The test therefore removes its own rows afterwards, so other test classes that delete
 * merchants don't hit the payment_requests foreign key.
 */
@SpringBootTest
@Import(TestcontainersConfiguration.class)
class PaymentRequestOptimisticLockingIntegrationTest {

    @Autowired
    private PaymentRequestService service;

    @Autowired
    private PaymentRequestRepository repository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PlatformTransactionManager transactionManager;

    @Autowired
    private Clock clock;

    private UUID merchantId;

    @AfterEach
    void removeTestData() {
        if (merchantId != null) {
            jdbcTemplate.update("delete from payment_requests where merchant_id = ?", merchantId);
            jdbcTemplate.update("delete from merchants where id = ?", merchantId);
        }
    }

    @Test
    void secondOfTwoConcurrentPaymentsIsRejectedByTheVersionCheck() {
        merchantId = insertActiveMerchant();
        UUID id = service.create(merchantId, Money.eur(1250), "Order", "REF-LOCK-1", null).getId();

        TransactionTemplate transactionA = new TransactionTemplate(transactionManager);
        TransactionTemplate transactionB = new TransactionTemplate(transactionManager);
        transactionB.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);

        assertThatThrownBy(() -> transactionA.executeWithoutResult(status -> {
            // A reads the request while it is still CREATED, at version 0
            PaymentRequest seenByA = repository.findById(id).orElseThrow();

            // B pays the same request in its own transaction and commits first: version 0 -> 1
            transactionB.executeWithoutResult(inner -> service.pay(id));

            // A still holds version 0, so its UPDATE ... WHERE version = 0 matches no row
            seenByA.pay(clock);
            repository.saveAndFlush(seenByA);
        })).isInstanceOf(ObjectOptimisticLockingFailureException.class);

        PaymentRequest stored = repository.findById(id).orElseThrow();
        assertThat(stored.statusAt(clock.instant())).isEqualTo(PaymentRequestStatus.PAID);
        assertThat(stored.getVersion()).isEqualTo(1L);
    }

    private UUID insertActiveMerchant() {
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