package com.cardrive.scheduling.outbox;

import com.cardrive.scheduling.domain.entity.OutboxEvent;
import com.cardrive.scheduling.kafka.KafkaTopics;
import com.cardrive.scheduling.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Polls unpublished outbox rows and publishes them to Kafka. Runs on a fixed
 * schedule, entirely decoupled from request-handling transactions - this is
 * where "slow" broker interaction happens, never inside @Transactional booking code.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private static final int BATCH_SIZE = 100;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
//    private final KafkaTemplate<String, Object> kafkaTemplate;
    @Scheduled(fixedDelayString = "${outbox.publisher.fixed-delay-ms:2000}")
    public void publishPending() {
        List<OutboxEvent> pending = outboxEventRepository.findByPublishedFalseOrderByIdAsc(PageRequest.of(0, BATCH_SIZE));
        for (OutboxEvent event : pending) {
            publishOne(event);
        }
    }

    @Transactional
    protected void publishOne(OutboxEvent event) {
        try {
            // Key by aggregateId so all events for the same test drive stay ordered on one partition.
            kafkaTemplate.send(KafkaTopics.TEST_DRIVE_EVENTS, event.getAggregateId(), event.getPayload()).get();
            event.setPublished(true);
            event.setPublishedAt(Instant.now());
            outboxEventRepository.save(event);
        } catch (Exception e) {
            log.error("Failed to publish outbox event id={} eventType={}, will retry next cycle",
                    event.getId(), event.getEventType(), e);
        }
    }
}
