package com.autoplatform.payment.client;

import java.math.BigDecimal;

public record OfferResponse(
        String offerId,
        String carId,
        String customerId,
        BigDecimal offeredAmount,
        String status
) {
}