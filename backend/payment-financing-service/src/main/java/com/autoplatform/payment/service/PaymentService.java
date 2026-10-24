package com.autoplatform.payment.service;

import com.autoplatform.payment.entity.Order;
import com.autoplatform.payment.entity.Payment;

public interface PaymentService {

    /** Creates a Stripe PaymentIntent for the order's chargeable amount and persists the corresponding Payment row. */
    Payment createPaymentIntent(Order order);

    /** Issues a full refund for a Stripe PaymentIntent and updates the Payment row's status. */
    void refund(String stripePaymentIntentId);

    /**
     * Cancels a not-yet-completed PaymentIntent (abandoned checkout).
     * Throws PaymentAlreadyInFlightException instead of cancelling if the
     * intent has already succeeded or is mid-processing on Stripe's side -
     * the caller should leave that order alone and let the webhook resolve it.
     */
    void cancelPaymentIntent(String stripePaymentIntentId);
}
