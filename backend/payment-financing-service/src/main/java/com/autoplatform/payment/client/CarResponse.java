package com.autoplatform.payment.client;

import java.math.BigDecimal;

/**
 * Shape of the Car Service's GET /api/v1/cars/{id} response, as far as this
 * service needs it. Adjust field names/types here to match your actual Car
 * Service DTO - this is the single place that mapping lives.
 */
public record CarResponse(
        Long id,
        Long sellerId,
        BigDecimal price,
        String currency,
        boolean listed
) {
}
