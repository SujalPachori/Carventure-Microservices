package com.autoplatform.payment.event;

import com.autoplatform.payment.enums.OrderStatus;
import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.Instant;

/** General order-lifecycle event, published to "order-events" for audit/history consumers. */
public record OrderEvent(
        String eventType,
        Long orderId,
        Long buyerId,
        Long sellerId,
        Long carId,
        BigDecimal amount,
        String currency,
        OrderStatus status,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant occurredAt
) {
    public static final String CREATED = "ORDER_CREATED";
    public static final String PAID = "ORDER_PAID";
    public static final String FINANCING_ACTIVATED = "ORDER_FINANCING_ACTIVATED";
    public static final String PAYMENT_FAILED = "ORDER_PAYMENT_FAILED";
    public static final String CANCELLED = "ORDER_CANCELLED";
    public static final String REFUNDED = "ORDER_REFUNDED";
    public static final String EXPIRED = "ORDER_EXPIRED";
}
