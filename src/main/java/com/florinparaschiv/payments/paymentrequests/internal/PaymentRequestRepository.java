package com.florinparaschiv.payments.paymentrequests.internal;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface PaymentRequestRepository extends JpaRepository<PaymentRequest, UUID> {

    Optional<PaymentRequest> findByIdAndMerchantId(UUID id, UUID merchantId);
}