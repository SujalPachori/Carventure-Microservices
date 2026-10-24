package com.autoplatform.payment.paymentkafka.kafka;

import com.autoplatform.payment.paymentkafka.event.OrderCompletedEvent;
import com.autoplatform.payment.paymentkafka.event.OrderCreatedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes Payment/Order domain events for the Reporting service to consume.
 * Call publishOrderCreated(...) right after an order is persisted, and
 * publishOrderCompleted(...) once payment settles / order status flips to COMPLETED.
 */
@Component
@RequiredArgsConstructor
public class OrderEventProducer {

    public static final String TOPIC_ORDER_CREATED = "order.created";
    public static final String TOPIC_ORDER_COMPLETED = "order.completed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishOrderCreated(OrderCreatedEvent event) {
        kafkaTemplate.send(TOPIC_ORDER_CREATED, String.valueOf(event.getOrderId()), event);
    }

    public void publishOrderCompleted(OrderCompletedEvent event) {
        kafkaTemplate.send(TOPIC_ORDER_COMPLETED, String.valueOf(event.getOrderId()), event);
    }
}
