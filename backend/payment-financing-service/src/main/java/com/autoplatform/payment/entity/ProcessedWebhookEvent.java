package com.autoplatform.payment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Tracks Stripe event IDs already handled, so a redelivered webhook (Stripe
 * retries on anything but a 2xx, and can also just double-send) is a no-op
 * instead of double-processing a payment. The primary key IS the guard:
 * a second insert of the same event id fails fast with a constraint violation.
 */
@Entity
@Table(name = "processed_webhook_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedWebhookEvent {

    @Id
    @Column(name = "stripe_event_id", length = 100)
    private String stripeEventId;

    @Column(name = "event_type", nullable = false, length = 100)
    private String eventType;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;
}
