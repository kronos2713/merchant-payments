package com.florinparaschiv.payments.paymentrequests.internal;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

interface PaymentRequestRepository extends JpaRepository<PaymentRequest, UUID>,
        JpaSpecificationExecutor<PaymentRequest> {

    Optional<PaymentRequest> findByIdAndMerchantId(UUID id, UUID merchantId);
}