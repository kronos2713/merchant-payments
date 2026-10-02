package com.florinparaschiv.payments.merchants.internal;

import com.florinparaschiv.payments.merchants.MerchantDirectory;
import com.florinparaschiv.payments.merchants.MerchantSummary;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
class JpaMerchantDirectory implements MerchantDirectory {

    private final MerchantRepository repository;

    JpaMerchantDirectory(MerchantRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<MerchantSummary> findById(UUID merchantId) {
        return repository.findById(merchantId)
                .map(merchant -> new MerchantSummary(
                        merchant.getId(),
                        merchant.getLegalName(),
                        merchant.getStatus()));
    }
}