package com.autoplatform.payment.service.impl;

import com.autoplatform.payment.client.*;
import com.autoplatform.payment.config.FinancingProperties;
import com.autoplatform.payment.config.OrderExpiryProperties;
import com.autoplatform.payment.entity.FinancingPlan;
import com.autoplatform.payment.entity.Order;
import com.autoplatform.payment.entity.Payment;
import com.autoplatform.payment.enums.FinancingStatus;
import com.autoplatform.payment.enums.OrderStatus;
import com.autoplatform.payment.enums.PaymentType;
import com.autoplatform.payment.event.OrderEvent;
import com.autoplatform.payment.event.PaymentEventProducer;
import com.autoplatform.payment.exception.*;
import com.autoplatform.payment.paymentkafka.event.OrderCompletedEvent;
import com.autoplatform.payment.paymentkafka.event.OrderCreatedEvent;
import com.autoplatform.payment.paymentkafka.kafka.OrderEventProducer;
import com.autoplatform.payment.repository.FinancingPlanRepository;
import com.autoplatform.payment.repository.OrderRepository;
import com.autoplatform.payment.repository.PaymentRepository;
import com.autoplatform.payment.service.OrderService;
import com.autoplatform.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Owns the Order lifecycle. The guarantee that matters here mirrors the
 * Scheduling Service's double-booking guarantee: "no matter how many orders
 * are placed for the same car, at most one can ever finish as sold." That's
 * enforced the same two-layer way -
 *   - application check (findByCarIdAndStatusIn before creating a new order)
 *   - database constraint (partial unique index on orders.car_id, see V1__init.sql)
 * with completePayment() catching the DB-level violation and automatically
 * refunding the losing payment rather than leaving a charged buyer stuck.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderServiceImpl implements OrderService {

    private static final List<OrderStatus> BLOCKING_STATUSES =
            List.of(OrderStatus.CREATED, OrderStatus.AWAITING_PAYMENT, OrderStatus.PAID, OrderStatus.FINANCING_ACTIVE);

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final FinancingPlanRepository financingPlanRepository;
    private final CarValidationService carValidationService;
    private final PaymentService paymentService;
    private final PaymentEventProducer eventProducer;
    private final FinancingProperties financingProperties;
    private final OrderExpiryProperties orderExpiryProperties;
    private final OrderEventProducer orderEventProducer;
    private final OfferServiceClient offerServiceClient;
    private final CarServiceClient carServiceClient;

    @Override
    public Order createOrder(Long buyerId, Long carId, PaymentType paymentType,
                             Integer downPaymentPercent, Integer tenureMonths, String offerId) {

        // Confirms the car is real, still listed, and tells us who the seller actually is -
        // never trust a client-supplied sellerId (see client/CarValidationService).
        CarResponse car = carValidationService.getCarOrThrow(carId);
        BigDecimal agreedPrice = resolveAgreedPrice(car, carId, offerId);
        boolean holdsAcceptedOffer = offerId != null && !offerId.isBlank();

        if (!car.listed() && !holdsAcceptedOffer) {
            throw new InvalidRequestException("Car " + carId + " is no longer listed for sale");
        }

        Optional<Order> blocking = orderRepository.findByCarIdAndStatusIn(carId, BLOCKING_STATUSES);
        if (blocking.isPresent()) {
            Order existing = blocking.get();
            boolean reclaimed = !existing.getStatus().isSold() && isExpired(existing)
                    && tryExpire(existing, "superseded by a new purchase attempt for the same car");
            if (!reclaimed) {
                throw new CarAlreadySoldException("Car " + carId + " already has an active or completed order");
            }
        }

        Order order = buildOrder(buyerId, car, agreedPrice, paymentType, downPaymentPercent, tenureMonths);
        order.setStatus(OrderStatus.CREATED);
        order.setExpiresAt(Instant.now().plus(orderExpiryProperties.expiryMinutes(), ChronoUnit.MINUTES));
        order = saveOrder(order);

        eventProducer.publishOrderEvent(OrderEvent.CREATED, order);

        Payment payment = paymentService.createPaymentIntent(order);
        order.setStatus(OrderStatus.AWAITING_PAYMENT);
        order = saveOrder(order);

        log.info("Created orderId={} paymentIntentId={} for carId={} buyerId={} amount={}",
                order.getId(), payment.getStripePaymentIntentId(), carId, buyerId, order.getTotalAmount());

        orderEventProducer.publishOrderCreated(
                new OrderCreatedEvent(
                        order.getId(),
                        order.getCarId(),
                        order.getBuyerId().toString(),
                        order.getSellerId().toString(),
                        Double.parseDouble(order.getTotalAmount().toString()),
                        order.getStatus().toString(),
                        LocalDateTime.now()
                )
        );
        return order;
    }

    private Order buildOrder(Long buyerId, CarResponse car, BigDecimal price, PaymentType paymentType,
                             Integer downPaymentPercent, Integer tenureMonths) {
        Order.OrderBuilder builder = Order.builder()
                .buyerId(buyerId)
                .sellerId(car.sellerId())
                .carId(car.id())
                .totalAmount(price)
                .currency(car.currency())
                .paymentType(paymentType);

        if (paymentType == PaymentType.FINANCING) {
            int pct = downPaymentPercent != null ? downPaymentPercent : financingProperties.defaultDownPaymentPercent();
            BigDecimal downPayment = price
                    .multiply(BigDecimal.valueOf(pct))
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            builder.downPaymentAmount(downPayment);
        }

        return builder.build();
    }

    @Override
    @Transactional(readOnly = true)
    public String getClientSecret(Long orderId) {
        Order order = getById(orderId);
        List<Payment> payments = paymentRepository.findByOrderId(orderId);
        Payment latest = payments.stream()
                .max((a, b) -> a.getCreatedAt().compareTo(b.getCreatedAt()))
                .orElseThrow(() -> new ResourceNotFoundException("No payment attempt found for order " + orderId));
        return retrieveClientSecret(latest.getStripePaymentIntentId());
    }

    private String retrieveClientSecret(String stripePaymentIntentId) {
        try {
            return com.stripe.model.PaymentIntent.retrieve(stripePaymentIntentId).getClientSecret();
        } catch (com.stripe.exception.StripeException e) {
            throw new com.autoplatform.payment.exception.PaymentProcessingException(
                    "Failed to retrieve client secret for " + stripePaymentIntentId, e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Order getById(Long orderId) {
        return orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order " + orderId + " not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getForBuyer(Long buyerId) {
        return orderRepository.findByBuyerIdOrderByCreatedAtDesc(buyerId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Order> getForSeller(Long sellerId) {
        return orderRepository.findBySellerIdOrderByCreatedAtDesc(sellerId);
    }

    @Override
    public Order cancel(Long orderId, Long actingUserId) {
        Order order = getById(orderId);
        if (!order.getBuyerId().equals(actingUserId)) {
            throw new InvalidStateTransitionException("Only the buyer may cancel this order");
        }
        if (order.getStatus() != OrderStatus.CREATED && order.getStatus() != OrderStatus.AWAITING_PAYMENT) {
            throw new InvalidStateTransitionException("Cannot cancel an order in status " + order.getStatus());
        }
        order.setStatus(OrderStatus.CANCELLED);
        order = orderRepository.save(order);
        eventProducer.publishOrderEvent(OrderEvent.CANCELLED, order);
        return order;
    }

    @Override
    public Order completePayment(Long orderId, String stripePaymentIntentId) {
        // Lock the order row so a redelivered/duplicate webhook for the same
        // order can't race this method against itself.
        Order order = orderRepository.findAndLockById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order " + orderId + " not found"));

        if (order.getStatus().isSold()) {
            log.info("orderId={} already marked sold (status={}) - webhook redelivery, no-op", orderId, order.getStatus());
            return order;
        }
        if (order.getStatus() != OrderStatus.AWAITING_PAYMENT && order.getStatus() != OrderStatus.CREATED) {
            // The order already reached a terminal-but-unsold state (EXPIRED/CANCELLED/
            // PAYMENT_FAILED/REFUNDED) - meaning we already gave up on it and may well
            // have let someone else buy the car. A late success for it must not resurrect
            // it to PAID; refund the money instead and leave its status where it was.
            log.warn("orderId={} received a late payment success while in terminal status={} - refunding rather than reviving",
                    orderId, order.getStatus());
            paymentService.refund(stripePaymentIntentId);
            return order;
        }

        boolean financed = order.getPaymentType() == PaymentType.FINANCING;
        order.setStatus(financed ? OrderStatus.FINANCING_ACTIVE : OrderStatus.PAID);

        try {
            order = saveOrder(order);
        } catch (DataIntegrityViolationException e) {
            // Lost the race: another order already sold this car. The buyer's money
            // is already captured by Stripe, so refund it automatically rather than
            // leaving them charged for a car they didn't get.
            log.error("orderId={} lost the race for carId={} - refunding paymentIntentId={}",
                    orderId, order.getCarId(), stripePaymentIntentId);
            paymentService.refund(stripePaymentIntentId);
            order.setStatus(OrderStatus.REFUNDED);
            order = orderRepository.save(order);
            eventProducer.publishOrderEvent(OrderEvent.REFUNDED, order);
            return order;
        }

        if (financed) {
            createFinancingPlan(order);
            eventProducer.publishOrderEvent(OrderEvent.FINANCING_ACTIVATED, order);
        } else {
            eventProducer.publishOrderEvent(OrderEvent.PAID, order);
        }

        // The event that actually notifies the seller their car is sold.
        eventProducer.publishCarSold(order);
        try {
            carServiceClient.updateStatus(order.getCarId(), "SOLD");
        } catch (Exception ex) {
            log.error("Order {} is paid but car {} could not be marked SOLD: {}",
                    order.getId(), order.getCarId(), ex.getMessage());
        }
        orderEventProducer.publishOrderCompleted(
                new OrderCompletedEvent(
                        order.getId(),
                        order.getCarId(),
                        order.getBuyerId().toString(),
                        order.getSellerId().toString(),
                        Double.parseDouble(order.getTotalAmount().toString()),
                        LocalDateTime.now()
                )
        );

        return order;
    }

    private void createFinancingPlan(Order order) {
        BigDecimal principal = order.getTotalAmount().subtract(order.getDownPaymentAmount());
        int tenure = financingProperties.defaultTenureMonths();
        BigDecimal monthlyInstallment = principal.divide(BigDecimal.valueOf(tenure), 2, RoundingMode.HALF_UP);

        financingPlanRepository.save(FinancingPlan.builder()
                .orderId(order.getId())
                .principalAmount(principal)
                .tenureMonths(tenure)
                .monthlyInstallment(monthlyInstallment)
                .status(FinancingStatus.ACTIVE)
                .build());
    }

    @Override
    public Order failPayment(Long orderId, String stripePaymentIntentId, String reason) {
        Order order = orderRepository.findAndLockById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order " + orderId + " not found"));

        if (order.getStatus().isTerminalOrSold()) {
            log.info("orderId={} already in terminal status={}, ignoring late payment_failed", orderId, order.getStatus());
            return order;
        }

        order.setStatus(OrderStatus.PAYMENT_FAILED);
        order = orderRepository.save(order);
        log.warn("orderId={} payment failed: {}", orderId, reason);
        eventProducer.publishOrderEvent(OrderEvent.PAYMENT_FAILED, order);
        return order;
    }

    /** Persists the order, translating the partial-unique-index violation on (car_id) WHERE sold into a clean domain exception upstream. */
    private Order saveOrder(Order order) {
        return orderRepository.save(order);
    }

    @Override
    public int expireStaleOrders() {
        List<Order> stale = orderRepository.findByStatusInAndExpiresAtBefore(
                List.of(OrderStatus.CREATED, OrderStatus.AWAITING_PAYMENT), Instant.now());

        int expiredCount = 0;
        for (Order order : stale) {
            if (tryExpire(order, "expired after " + orderExpiryProperties.expiryMinutes() + " minutes without payment")) {
                expiredCount++;
            }
        }
        return expiredCount;
    }

    private boolean isExpired(Order order) {
        return order.getExpiresAt() != null && order.getExpiresAt().isBefore(Instant.now());
    }

    private boolean tryExpire(Order order, String reason) {
        Optional<Payment> latestPayment = paymentRepository.findByOrderId(order.getId()).stream()
                .max(Comparator.comparing(Payment::getCreatedAt));

        if (latestPayment.isPresent()) {
            try {
                paymentService.cancelPaymentIntent(latestPayment.get().getStripePaymentIntentId());
            } catch (PaymentAlreadyInFlightException e) {
                log.info("orderId={} not expiring - {}", order.getId(), e.getMessage());
                return false;
            } catch (RuntimeException e) {
                log.warn("orderId={} failed to cancel Stripe PaymentIntent {} while expiring: {}",
                        order.getId(), latestPayment.get().getStripePaymentIntentId(), e.getMessage());
            }
        }

        order.setStatus(OrderStatus.EXPIRED);
        orderRepository.save(order);
        log.info("orderId={} expired ({})", order.getId(), reason);
        eventProducer.publishOrderEvent(OrderEvent.EXPIRED, order);
        return true;
    }

    private BigDecimal resolveAgreedPrice(CarResponse car, Long carId, String offerId) {

        if (offerId == null || offerId.isBlank()) {
            return car.price();
        }

        OfferResponse offer = offerServiceClient.getOffer(offerId);

        if (!"ACCEPTED".equals(offer.status())) {
            throw new InvalidRequestException("Offer " + offerId + " is not accepted");
        }
        if (!String.valueOf(carId).equals(offer.carId())) {
            throw new InvalidRequestException("Offer " + offerId + " is for a different car");
        }

        String caller = SecurityContextHolder.getContext().getAuthentication().getName();
        if (!caller.equals(offer.customerId())) {
            throw new InvalidRequestException("Offer " + offerId + " does not belong to you");
        }

        log.info("Using negotiated price {} from offerId={} instead of listed price {}",
                offer.offeredAmount(), offerId, car.price());

        return offer.offeredAmount();
    }
}
