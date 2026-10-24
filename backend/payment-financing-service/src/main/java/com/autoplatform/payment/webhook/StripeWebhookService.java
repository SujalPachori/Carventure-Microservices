package com.autoplatform.payment.webhook;

import com.autoplatform.payment.config.StripeProperties;
import com.autoplatform.payment.entity.ProcessedWebhookEvent;
import com.autoplatform.payment.exception.InvalidWebhookSignatureException;
import com.autoplatform.payment.exception.PaymentProcessingException;
import com.autoplatform.payment.repository.ProcessedWebhookEventRepository;
import com.autoplatform.payment.service.OrderService;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import com.stripe.net.Webhook;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

/**
 * Verifies and dispatches Stripe webhook events.
 *
 * Two independent safety nets against duplicate processing, since Stripe
 * both retries on non-2xx responses AND can occasionally deliver an event
 * more than once even on success:
 *   1. ProcessedWebhookEvent - the Stripe event id is the primary key, so a
 *      second delivery of the same event fails the insert and is skipped.
 *   2. OrderService.completePayment()/failPayment() are themselves
 *      idempotent (no-op if the order is already in a terminal/sold state).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StripeWebhookService {

    private final StripeProperties stripeProperties;
    private final ProcessedWebhookEventRepository processedWebhookEventRepository;
    private final OrderService orderService;

    public void handle(String payload, String sigHeader) {
        Event event;
        try {
            event = Webhook.constructEvent(payload, sigHeader, stripeProperties.webhookSecret());
        } catch (SignatureVerificationException e) {
            log.warn("Rejected webhook with invalid signature: {}", e.getMessage());
            throw new InvalidWebhookSignatureException("Invalid Stripe webhook signature");
        }

        if (!markProcessedIfNew(event)) {
            log.info("Ignoring duplicate delivery of Stripe event id={} type={}", event.getId(), event.getType());
            return;
        }

        switch (event.getType()) {
            case "payment_intent.succeeded" -> handlePaymentSucceeded(resolvePaymentIntent(event));
            case "payment_intent.payment_failed" -> handlePaymentFailed(resolvePaymentIntent(event));
            default -> log.debug("Ignoring unhandled Stripe event type={}", event.getType());
        }
    }

    /**
     * Stripe's automatic Gson-based deserialization of the webhook payload
     * (getDataObjectDeserializer().getObject()) can silently come back empty
     * when the Stripe account/API version generating the event is newer than
     * what this SDK version's object mapper understands - it does not throw,
     * it just returns Optional.empty(). Rather than trust that best-effort
     * deserialization, pull the object's id straight out of the raw JSON
     * (which is always present regardless of API version skew) and fetch it
     * fresh via the API - a plain retrieve-by-id always succeeds.
     */
    private PaymentIntent resolvePaymentIntent(Event event) {
        StripeObject deserialized = event.getDataObjectDeserializer().getObject().orElse(null);
        if (deserialized instanceof PaymentIntent intent) {
            return intent;
        }

        String rawJson = event.getDataObjectDeserializer().getRawJson();
        String id;
        try {
            JsonObject json = JsonParser.parseString(rawJson).getAsJsonObject();
            id = json.get("id").getAsString();
        } catch (RuntimeException e) {
            throw new PaymentProcessingException(
                    "Could not extract PaymentIntent id from event " + event.getId() + " raw payload", e);
        }

        log.warn("Deserialization of event {} came back empty (likely API version skew) - " +
                "falling back to fetching PaymentIntent {} fresh", event.getId(), id);
        try {
            return PaymentIntent.retrieve(id);
        } catch (StripeException e) {
            throw new PaymentProcessingException("Failed to retrieve PaymentIntent " + id + " after deserialization fallback", e);
        }
    }

    /** @return true if this event id hadn't been seen before (and should be processed), false if it's a duplicate delivery. */
    @Transactional
    protected boolean markProcessedIfNew(Event event) {
        try {
            processedWebhookEventRepository.save(new ProcessedWebhookEvent(event.getId(), event.getType(), Instant.now()));
            return true;
        } catch (DataIntegrityViolationException e) {
            return false;
        }
    }

    private void handlePaymentSucceeded(PaymentIntent intent) {
        Long orderId = orderIdFromMetadata(intent);
        if (orderId == null) return;

        orderService.completePayment(orderId, intent.getId());
    }

    private void handlePaymentFailed(PaymentIntent intent) {
        Long orderId = orderIdFromMetadata(intent);
        if (orderId == null) return;

        String reason = Optional.ofNullable(intent.getLastPaymentError())
                .map(e -> e.getMessage())
                .orElse("unknown");
        orderService.failPayment(orderId, intent.getId(), reason);
    }

    private Long orderIdFromMetadata(PaymentIntent intent) {
        String raw = intent.getMetadata() == null ? null : intent.getMetadata().get("orderId");
        if (raw == null) {
            log.error("PaymentIntent {} has no orderId metadata - cannot correlate to an order", intent.getId());
            return null;
        }
        try {
            return Long.valueOf(raw);
        } catch (NumberFormatException e) {
            log.error("PaymentIntent {} has malformed orderId metadata: {}", intent.getId(), raw);
            return null;
        }
    }
}
