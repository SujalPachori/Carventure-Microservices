package com.autoplatform.payment.dto.response;

import com.autoplatform.payment.enums.FinancingStatus;

import java.math.BigDecimal;

public record FinancingPlanResponse(
        Long id,
        Long orderId,
        BigDecimal principalAmount,
        Integer tenureMonths,
        BigDecimal monthlyInstallment,
        FinancingStatus status
) {
}
