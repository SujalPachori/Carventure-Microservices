package com.autoplatform.payment.event;

import com.autoplatform.payment.entity.Order;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;

/**
 * Publishes order-lifecycle and car-sold events to Kafka.
 *
 * Publishing happens inside the same @Transactional method that persists the
 * order/payment state change, same fire-and-forget-with-logging approach as
 * the Scheduling Service. For guaranteed delivery even across a crash between
 * DB commit and publish, promote to a transactional outbox - see README.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${payment.kafka.topic.order-events}")
    private String orderEventsTopic;

    @Value("${payment.kafka.topic.car-sold-events}")
    private String carSoldEventsTopic;

    public void publishOrderEvent(String eventType, Order order) {
        OrderEvent event = new OrderEvent(
                eventType, order.getId(), order.getBuyerId(), order.getSellerId(), order.getCarId(),
                order.getTotalAmount(), order.getCurrency(), order.getStatus(), Instant.now());

        send(orderEventsTopic, String.valueOf(order.getCarId()), event, eventType, order.getId());
    }

    /** The event that actually answers "notify the seller their car is sold". */
    public void publishCarSold(Order order) {
        CarSoldEvent event = new CarSoldEvent(
                order.getId(), order.getCarId(), order.getSellerId(), order.getBuyerId(),
                order.getTotalAmount(), order.getCurrency(), Instant.now());

        send(carSoldEventsTopic, String.valueOf(order.getCarId()), event, "CAR_SOLD", order.getId());
    }

    private void send(String topic, String key, Object payload, String eventType, Long orderId) {
        kafkaTemplate.send(topic, key, payload).whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish {} for orderId={}: {}", eventType, orderId, ex.getMessage(), ex);
            } else {
                log.debug("Published {} for orderId={} to partition={} offset={}",
                        eventType, orderId, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            }
        });
    }
}
