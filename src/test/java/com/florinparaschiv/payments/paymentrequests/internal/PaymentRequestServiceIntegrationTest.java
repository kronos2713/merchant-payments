package com.florinparaschiv.payments.paymentrequests.internal;

import com.florinparaschiv.payments.TestcontainersConfiguration;
import com.florinparaschiv.payments.shared.Money;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Import(TestcontainersConfiguration.class)
@Transactional
class PaymentRequestServiceIntegrationTest {

    @Autowired
    private PaymentRequestService service;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Clock clock;

    @Test
    void createsRequestForActiveMerchant() {
        UUID merchantId = insertMerchant("ACTIVE");

        PaymentRequest created = service.create(merchantId, Money.eur(1250), "Order 1001", "ORDER-1001", null);

        assertThat(service.get(merchantId, created.getId()).getMerchantReference()).isEqualTo("ORDER-1001");
    }

    @Test
    void cancelsRequest() {
        UUID merchantId = insertMerchant("ACTIVE");
        UUID id = service.create(merchantId, Money.eur(1250), "Order", "REF-1", null).getId();

        PaymentRequest cancelled = service.cancel(merchantId, id);

        assertThat(cancelled.statusAt(clock.instant())).isEqualTo(PaymentRequestStatus.CANCELLED);
    }

    @Test
    void hidesRequestFromOtherMerchant() {
        UUID owner = insertMerchant("ACTIVE");
        UUID other = insertMerchant("ACTIVE");
        UUID id = service.create(owner, Money.eur(1250), "Order", "REF-1", null).getId();

        assertThatThrownBy(() -> service.cancel(other, id))
                .isInstanceOf(PaymentRequestNotFoundException.class);
    }
    @Test
    void rejectsUnknownMerchant() {
        assertThatThrownBy(() -> service.create(UUID.randomUUID(), Money.eur(1250), "Order", "REF-1", null))
                .isInstanceOf(UnknownMerchantException.class);
    }

    @Test
    void rejectsPendingMerchant() {
        UUID merchantId = insertMerchant("PENDING");

        assertThatThrownBy(() -> service.create(merchantId, Money.eur(1250), "Order", "REF-1", null))
                .isInstanceOf(PaymentRequestConflictException.class);
    }

    @Test
    void translatesDuplicateReference() {
        UUID merchantId = insertMerchant("ACTIVE");
        service.create(merchantId, Money.eur(1250), "Order", "REF-1", null);

        assertThatThrownBy(() -> service.create(merchantId, Money.eur(999), "Other", "REF-1", null))
                .isInstanceOf(PaymentRequestAlreadyExistsException.class);
    }

    @Test
    void paysRequestAndBumpsVersion() {
        UUID merchantId = insertMerchant("ACTIVE");
        UUID id = service.create(merchantId, Money.eur(1250), "Order", "REF-1", null).getId();

        PaymentRequest paid = service.pay(id);
        entityManager.flush();

        assertThat(paid.statusAt(clock.instant())).isEqualTo(PaymentRequestStatus.PAID);
        assertThat(paid.getVersion()).isEqualTo(1L);
    }

    @Test
    void rejectsPaymentWhileMerchantSuspended() {
        UUID merchantId = insertMerchant("ACTIVE");
        UUID id = service.create(merchantId, Money.eur(1250), "Order", "REF-1", null).getId();
        jdbcTemplate.update("update merchants set status = 'SUSPENDED' where id = ?", merchantId);
        entityManager.clear();

        assertThatThrownBy(() -> service.pay(id))
                .isInstanceOf(PaymentRequestConflictException.class)
                .hasMessageContaining("cannot be paid at the moment");
    }

    @Test
    void rejectsUnknownPaymentRequest() {
        assertThatThrownBy(() -> service.get(UUID.randomUUID(), UUID.randomUUID()))
                .isInstanceOf(PaymentRequestNotFoundException.class);
    }

    private UUID insertMerchant(String status) {
        UUID id = UUID.randomUUID();
        String registrationNumber =
                String.format("%08d", ThreadLocalRandom.current().nextInt(100_000_000));
        jdbcTemplate.update("""
                insert into merchants (id, legal_name, registration_country, registration_number,
                                       settlement_iban, status, version, created_at, updated_at)
                values (?, 'Test Shop B.V.', 'NL', ?, 'NL91ABNA0417164300', ?, 0, now(), now())
                """, id, registrationNumber, status);
        return id;
    }
}