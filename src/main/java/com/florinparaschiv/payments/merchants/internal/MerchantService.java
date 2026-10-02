package com.florinparaschiv.payments.merchants.internal;

import java.time.Clock;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;

@Service
class MerchantService {

    private static final Sort NEWEST_FIRST =
            Sort.by(Sort.Order.desc("createdAt"), Sort.Order.asc("id"));

    private final MerchantRepository repository;
    private final Clock clock;

    MerchantService(MerchantRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Transactional
    public MerchantResponse register(RegisterMerchantRequest request) {
        Merchant merchant = Merchant.register(
                request.legalName(),
                request.registrationCountry(),
                request.registrationNumber(),
                request.settlementIban(),
                clock);
        try {
            repository.saveAndFlush(merchant);
        } catch (DataIntegrityViolationException ex) {
            if (isUniqueViolation(ex, "uq_merchants_registration")) {
                throw new MerchantAlreadyRegisteredException(
                        merchant.getRegistrationCountry(), merchant.getRegistrationNumber());
            }
            throw ex;
        }
        return MerchantResponse.from(merchant);
    }

    @Transactional(readOnly = true)
    public MerchantResponse get(UUID merchantId) {
        return MerchantResponse.from(load(merchantId));
    }

    @Transactional(readOnly = true)
    public Page<MerchantResponse> list(int page, int size) {
        return repository.findAll(PageRequest.of(page, size, NEWEST_FIRST))
                .map(MerchantResponse::from);
    }

    @Transactional
    public MerchantResponse activate(UUID merchantId) {
        Merchant merchant = load(merchantId);
        merchant.activate(clock);
        return MerchantResponse.from(merchant);
    }

    @Transactional
    public MerchantResponse suspend(UUID merchantId) {
        Merchant merchant = load(merchantId);
        merchant.suspend(clock);
        return MerchantResponse.from(merchant);
    }

    @Transactional
    public MerchantResponse reactivate(UUID merchantId) {
        Merchant merchant = load(merchantId);
        merchant.reactivate(clock);
        return MerchantResponse.from(merchant);
    }

    private Merchant load(UUID merchantId) {
        return repository.findById(merchantId)
                .orElseThrow(() -> new MerchantNotFoundException(merchantId));
    }

    private static boolean isUniqueViolation(Throwable ex, String constraintName) {
        for (Throwable cause = ex; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation) {
                return constraintName.equalsIgnoreCase(violation.getConstraintName());
            }
        }
        return false;
    }
}