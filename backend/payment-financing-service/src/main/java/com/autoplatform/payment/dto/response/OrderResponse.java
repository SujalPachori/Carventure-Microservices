package com.autoplatform.payment.dto.response;

import com.autoplatform.payment.enums.OrderStatus;
import com.autoplatform.payment.enums.PaymentType;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(
        Long id,
        Long buyerId,
        Long sellerId,
        Long carId,
        BigDecimal totalAmount,
        BigDecimal downPaymentAmount,
        String currency,
        PaymentType paymentType,
        OrderStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
