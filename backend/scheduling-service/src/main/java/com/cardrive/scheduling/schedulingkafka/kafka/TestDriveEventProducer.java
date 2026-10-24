package com.cardrive.scheduling.schedulingkafka.kafka;

import com.cardrive.scheduling.schedulingkafka.event.TestDriveCompletedEvent;
import com.cardrive.scheduling.schedulingkafka.event.TestDriveScheduledEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes Scheduling domain events for the Reporting service to consume.
 * Call publishTestDriveScheduled(...) when a test drive is booked, and
 * publishTestDriveCompleted(...) when it's marked done - this is what
 * Reporting joins against completed orders to compute the conversion rate.
 */
@Component
@RequiredArgsConstructor
public class TestDriveEventProducer {

    public static final String TOPIC_TEST_DRIVE_SCHEDULED = "testdrive.scheduled";
    public static final String TOPIC_TEST_DRIVE_COMPLETED = "testdrive.completed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishTestDriveScheduled(TestDriveScheduledEvent event) {
        kafkaTemplate.send(TOPIC_TEST_DRIVE_SCHEDULED, String.valueOf(event.getTestDriveId()), event);
    }

    public void publishTestDriveCompleted(TestDriveCompletedEvent event) {
        kafkaTemplate.send(TOPIC_TEST_DRIVE_COMPLETED, String.valueOf(event.getTestDriveId()), event);
    }
}
