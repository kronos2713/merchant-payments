package com.florinparaschiv.payments;

import org.springframework.boot.SpringApplication;

public class TestMerchantPaymentsApplication {

    public static void main(String[] args) {
        SpringApplication.from(MerchantPaymentsApplication::main).with(TestcontainersConfiguration.class).run(args);
    }

}
