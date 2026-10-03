package com.florinparaschiv.payments.paymentrequests.internal;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ExpiryPolicy.class)
class PaymentRequestsConfig {
}