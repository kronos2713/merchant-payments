package com.florinparaschiv.payments.merchants;

import java.util.Optional;
import java.util.UUID;

public interface MerchantDirectory {

    Optional<MerchantSummary> findById(UUID merchantId);
}