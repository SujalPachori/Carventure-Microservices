package com.autoplatform.payment.service.impl;

import com.autoplatform.payment.entity.Order;
import com.autoplatform.payment.entity.Payment;
import com.autoplatform.payment.enums.PaymentStatus;
import com.autoplatform.payment.exception.PaymentProcessingException;
import com.autoplatform.payment.exception.ResourceNotFoundException;
import com.autoplatform.payment.repository.PaymentRepository;
import com.autoplatform.payment.service.PaymentService;
import com.stripe.exception.StripeException;
import com.stripe.model.PaymentIntent;
import com.stripe.model.Refund;
import com.stripe.net.RequestOptions;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.RefundCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final PaymentRepository paymentRepository;

    @Override
    @Transactional
    public Payment createPaymentIntent(Order order) {
        BigDecimal amount = order.chargeableAmount();
        long amountInSmallestUnit = toSmallestCurrencyUnit(amount, order.getCurrency());

        try {
            PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                    .setAmount(amountInSmallestUnit)
                    .setCurrency(order.getCurrency().toLowerCase())
                    .setAutomaticPaymentMethods(
                            PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                    .setEnabled(true)
                                    .build())
                    .putMetadata("orderId", String.valueOf(order.getId()))
                    .putMetadata("carId", String.valueOf(order.getCarId()))
                    .putMetadata("buyerId", String.valueOf(order.getBuyerId()))
                    .putMetadata("sellerId", String.valueOf(order.getSellerId()))
                    .build();

            // Idempotency key ties this PaymentIntent to this order, so a retried
            // "create order" call (e.g. buyer double-clicking, or a client retry
            // after a timeout) never creates a second charge for the same order.
            RequestOptions options = RequestOptions.builder()
                    .setIdempotencyKey("order-" + order.getId() + "payment-intent")
                    .build();

            PaymentIntent intent = PaymentIntent.create(params, options);

            Payment payment = Payment.builder()
                    .orderId(order.getId())
                    .stripePaymentIntentId(intent.getId())
                    .amount(amount)
                    .currency(order.getCurrency())
                    .status(mapStatus(intent.getStatus()))
                    .build();

            return paymentRepository.save(payment);
        } catch (StripeException e) {
            log.error("Stripe PaymentIntent creation failed for orderId={}: {}", order.getId(), e.getMessage(), e);
            throw new PaymentProcessingException("Failed to create Stripe payment intent for order " + order.getId(), e);
        }
    }

    @Override
    @Transactional
    public void refund(String stripePaymentIntentId) {
        Payment payment = paymentRepository.findByStripePaymentIntentId(stripePaymentIntentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment for intent " + stripePaymentIntentId + " not found"));

        try {
            RefundCreateParams params = RefundCreateParams.builder()
                    .setPaymentIntent(stripePaymentIntentId)
                    .build();
            RequestOptions options = RequestOptions.builder()
                    .setIdempotencyKey("refund-" + stripePaymentIntentId)
                    .build();
            Refund.create(params, options);

            payment.setStatus(PaymentStatus.REFUNDED);
            paymentRepository.save(payment);
        } catch (StripeException e) {
            log.error("Stripe refund failed for paymentIntentId={}: {}", stripePaymentIntentId, e.getMessage(), e);
            throw new PaymentProcessingException("Failed to refund payment intent " + stripePaymentIntentId, e);
        }
    }

    @Override
    @Transactional
    public void cancelPaymentIntent(String stripePaymentIntentId) {
        try {
            PaymentIntent intent = PaymentIntent.retrieve(stripePaymentIntentId);

            if ("succeeded".equals(intent.getStatus()) || "processing".equals(intent.getStatus())) {
                throw new com.autoplatform.payment.exception.PaymentAlreadyInFlightException(
                        "PaymentIntent " + stripePaymentIntentId + " is already " + intent.getStatus() + " - refusing to cancel");
            }

            intent.cancel();

            paymentRepository.findByStripePaymentIntentId(stripePaymentIntentId).ifPresent(payment -> {
                payment.setStatus(PaymentStatus.CANCELLED);
                paymentRepository.save(payment);
            });
        } catch (StripeException e) {
            log.error("Stripe cancel failed for paymentIntentId={}: {}", stripePaymentIntentId, e.getMessage(), e);
            throw new PaymentProcessingException("Failed to cancel payment intent " + stripePaymentIntentId, e);
        }
    }

    /** Stripe expects amounts in the currency's smallest unit (e.g. cents for USD); most supported currencies use 2 decimal places. */
    private long toSmallestCurrencyUnit(BigDecimal amount, String currency) {
        return amount.setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2)
                .longValueExact();
    }

    private PaymentStatus mapStatus(String stripeStatus) {
        return switch (stripeStatus) {
            case "succeeded" -> PaymentStatus.SUCCEEDED;
            case "processing" -> PaymentStatus.PROCESSING;
            case "requires_payment_method", "requires_confirmation", "requires_action", "requires_capture" ->
                    PaymentStatus.REQUIRES_PAYMENT_METHOD;
            case "canceled" -> PaymentStatus.FAILED;
            default -> PaymentStatus.REQUIRES_PAYMENT_METHOD;
        };
    }
}
