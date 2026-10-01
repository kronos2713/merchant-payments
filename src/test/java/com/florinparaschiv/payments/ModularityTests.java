package com.florinparaschiv.payments;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

    @Test
    void verifiesModularStructure() {
        ApplicationModules.of(MerchantPaymentsApplication.class).verify();
    }
}