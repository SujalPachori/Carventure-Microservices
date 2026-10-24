package com.autoplatform.payment.service;

import com.autoplatform.payment.entity.Order;
import com.autoplatform.payment.enums.PaymentType;

import java.util.List;

public interface OrderService {

    /** Creates the order and a Stripe PaymentIntent for its chargeable amount (full price, or down payment if financing). */
    Order createOrder(Long buyerId, Long carId, PaymentType paymentType, Integer downPaymentPercent, Integer tenureMonths, String offerId);

    /** Returns the current Stripe client secret for an order's live payment attempt, for the frontend to confirm payment. */
    String getClientSecret(Long orderId);

    Order getById(Long orderId);

    List<Order> getForBuyer(Long buyerId);

    List<Order> getForSeller(Long sellerId);

    Order cancel(Long orderId, Long actingUserId);

    /**
     * Finalizes an order after its Stripe PaymentIntent succeeded: marks it PAID
     * (or FINANCING_ACTIVE + creates the FinancingPlan), and publishes the
     * CAR_SOLD event. If another order already sold this car first, refunds
     * this payment instead and marks the order REFUNDED.
     */
    Order completePayment(Long orderId, String stripePaymentIntentId);

    Order failPayment(Long orderId, String stripePaymentIntentId, String reason);

    /**
     * Sweeps CREATED/AWAITING_PAYMENT orders past their expiry: cancels the
     * abandoned Stripe PaymentIntent (best-effort) and marks the order
     * EXPIRED, freeing its car for other buyers. Called periodically by
     * OrderExpiryScheduler. @return how many orders were expired.
     */
    int expireStaleOrders();
}
