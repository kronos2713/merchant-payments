package com.florinparaschiv.payments.paymentrequests.internal;

import com.florinparaschiv.payments.merchants.MerchantDirectory;
import com.florinparaschiv.payments.merchants.MerchantStatus;
import com.florinparaschiv.payments.merchants.MerchantSummary;
import com.florinparaschiv.payments.shared.ConstraintViolations;
import com.florinparaschiv.payments.shared.Money;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

@Service
class PaymentRequestService {

    static final String UNIQUE_MERCHANT_REFERENCE = "uq_payment_requests_merchant_reference";

    private final PaymentRequestRepository repository;
    private final MerchantDirectory merchantDirectory;
    private final ExpiryPolicy expiryPolicy;
    private final Clock clock;

    PaymentRequestService(PaymentRequestRepository repository, MerchantDirectory merchantDirectory,
                          ExpiryPolicy expiryPolicy, Clock clock) {
        this.repository = repository;
        this.merchantDirectory = merchantDirectory;
        this.expiryPolicy = expiryPolicy;
        this.clock = clock;
    }

    @Transactional
    public PaymentRequest create(UUID merchantId, Money amount, String description,
                                 String merchantReference, Instant requestedExpiresAt) {
        MerchantSummary merchant = merchantDirectory.findById(merchantId)
                .orElseThrow(() -> new UnknownMerchantException(merchantId));
        if (merchant.status() != MerchantStatus.ACTIVE) {
            throw new PaymentRequestConflictException("merchant cannot accept payment requests");
        }

        PaymentRequest request = PaymentRequest.create(merchantId, amount, description,
                merchantReference, requestedExpiresAt, expiryPolicy, clock);
        try {
            return repository.saveAndFlush(request);
        } catch (DataIntegrityViolationException e) {
            if (ConstraintViolations.isViolationOf(e, UNIQUE_MERCHANT_REFERENCE)) {
                throw new PaymentRequestAlreadyExistsException(merchantReference);
            }
            throw e;
        }
    }

    @Transactional(readOnly = true)
    public PaymentRequest get(UUID merchantId, UUID id) {
        return loadForMerchant(merchantId, id);
    }

    @Transactional(readOnly = true)
    public Page<PaymentRequest> list(UUID merchantId, PaymentRequestStatus status,
                                     String merchantReference, int page, int size, Instant now) {
        if (merchantDirectory.findById(merchantId).isEmpty()) {
            throw new UnknownMerchantException(merchantId);
        }

        Specification<PaymentRequest> spec = PaymentRequestSpecifications.belongsTo(merchantId);
        if (status != null) {
            spec = spec.and(PaymentRequestSpecifications.hasLogicalStatus(status, now));
        }
        if (merchantReference != null && !merchantReference.isBlank()) {
            spec = spec.and(PaymentRequestSpecifications.hasMerchantReference(merchantReference));
        }

        Pageable pageable = PageRequest.of(page, size,
                Sort.by(Sort.Direction.DESC, "createdAt", "id"));
        return repository.findAll(spec, pageable);
    }

    @Transactional
    public PaymentRequest pay(UUID id) {
        PaymentRequest request = load(id);
        boolean merchantActive = merchantDirectory.findById(request.getMerchantId())
                .map(merchant -> merchant.status() == MerchantStatus.ACTIVE)
                .orElse(false);
        if (!merchantActive) {
            throw new PaymentRequestConflictException("payment request cannot be paid at the moment");
        }
        request.pay(clock);
        return request;
    }

    @Transactional
    public PaymentRequest cancel(UUID merchantId, UUID id) {
        PaymentRequest request = loadForMerchant(merchantId, id);
        request.cancel(clock);
        return request;
    }

    private PaymentRequest load(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new PaymentRequestNotFoundException(id));
    }

    private PaymentRequest loadForMerchant(UUID merchantId, UUID id) {
        return repository.findByIdAndMerchantId(id, merchantId)
                .orElseThrow(() -> new PaymentRequestNotFoundException(id));
    }
}