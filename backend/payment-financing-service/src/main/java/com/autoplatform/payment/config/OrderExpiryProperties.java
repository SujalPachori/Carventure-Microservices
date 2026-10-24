package com.autoplatform.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "payment.order-expiry")
public record OrderExpiryProperties(
        Integer expiryMinutes
) {
    public OrderExpiryProperties {
        if (expiryMinutes == null) expiryMinutes = 30;
    }
}