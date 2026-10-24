package com.autoplatform.payment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Externalized defaults for the simplified in-house financing plan. */
@ConfigurationProperties(prefix = "payment.financing")
public record FinancingProperties(
        Integer defaultDownPaymentPercent,
        Integer defaultTenureMonths
) {
    public FinancingProperties {
        if (defaultDownPaymentPercent == null) defaultDownPaymentPercent = 20;
        if (defaultTenureMonths == null) defaultTenureMonths = 36;
    }
}
