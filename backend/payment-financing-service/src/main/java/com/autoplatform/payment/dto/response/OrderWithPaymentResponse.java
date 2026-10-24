package com.autoplatform.payment.dto.response;

@io.swagger.v3.oas.annotations.media.Schema(description = "An order plus the Stripe client secret the buyer's frontend needs to complete payment via Stripe Elements/Payment Element")
public record OrderWithPaymentResponse(
        OrderResponse order,
        String stripeClientSecret,
        String stripePublishableKeyHint
) {
}
