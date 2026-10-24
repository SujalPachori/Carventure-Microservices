package com.cardrive.scheduling.outbox;

import com.cardrive.scheduling.domain.entity.OutboxEvent;
import com.cardrive.scheduling.domain.entity.TestDrive;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds OutboxEvent rows for test-drive domain events. These are persisted in the
 * SAME transaction as the state change (see TestDriveService) - never published
 * directly to Kafka inline, so a slow/unavailable broker can never block or fail
 * a booking transaction.
 */
@Component
@RequiredArgsConstructor
public class OutboxEventFactory {

    private static final String AGGREGATE_TYPE = "TestDrive";

    private final ObjectMapper objectMapper;

    @SneakyThrows
    public OutboxEvent build(TestDrive testDrive, String eventType) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("testDriveId", testDrive.getId());
        payload.put("carId", testDrive.getCarId());
        payload.put("sellerUsername", testDrive.getSellerUsername());
        payload.put("buyerUsername", testDrive.getBuyerUsername());
        payload.put("startTime", testDrive.getStartTime().toString());
        payload.put("endTime", testDrive.getEndTime().toString());
        payload.put("status", testDrive.getStatus().name());
        payload.put("eventType", eventType);

        return OutboxEvent.builder()
                .aggregateType(AGGREGATE_TYPE)
                .aggregateId(String.valueOf(testDrive.getId()))
                .eventType(eventType)
                .payload(objectMapper.writeValueAsString(payload))
                .published(false)
                .build();
    }
}
