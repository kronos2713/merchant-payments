package com.florinparaschiv.payments.merchants.internal;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface MerchantRepository extends JpaRepository<Merchant, UUID> {
}