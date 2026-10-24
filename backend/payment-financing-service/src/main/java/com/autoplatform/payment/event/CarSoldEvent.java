package com.autoplatform.payment.event;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Published once, exactly when a car's sale is finalized (full payment
 * succeeded, or the down payment on a financed purchase succeeded). This is
 * the event the Notification Service consumes to tell the seller their car
 * is sold, and that Car Service consumes to delist the car. Kept separate
 * from the generic OrderEvent stream because it's a distinct downstream
 * contract with different consumers and stronger delivery expectations.
 */
public record CarSoldEvent(
        Long orderId,
        Long carId,
        Long sellerId,
        Long buyerId,
        BigDecimal amount,
        String currency,
        @JsonFormat(shape = JsonFormat.Shape.STRING) Instant soldAt
) {
}
