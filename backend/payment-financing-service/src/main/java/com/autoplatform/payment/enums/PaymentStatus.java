package com.autoplatform.payment.enums;

/** Mirrors the subset of Stripe PaymentIntent statuses this service cares about. */
public enum PaymentStatus {
    REQUIRES_PAYMENT_METHOD,
    PROCESSING,
    SUCCEEDED,
    FAILED,
    REFUNDED,
    CANCELLED
}
