package com.carventure.communication.service;

import com.carventure.communication.event.TestDriveEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import org.slf4j.Logger;

@Service
public class TestDriveEventListener {

    private static final Logger log = LoggerFactory.getLogger(TestDriveEventListener.class);

    private final NotificationService notificationService;
    private final ObjectMapper objectMapper;

    public TestDriveEventListener(NotificationService notificationService, ObjectMapper objectMapper) {
        this.notificationService = notificationService;
        this.objectMapper = objectMapper;
    }

    @KafkaListener(topics = "test-drive-events",
            groupId = "communication-testdrive-group",
            containerFactory = "stringListenerContainerFactory")
    public void onTestDriveEvent(String raw) {

        try {
            TestDriveEvent event = objectMapper.readValue(raw, TestDriveEvent.class);

            String slot = event.getStartTime() == null ? "" :
                    " for " + event.getStartTime().replace('T', ' ');

            switch (event.getEventType()) {

                case "TEST_DRIVE_REQUESTED" -> notify(event.getSellerUsername(), "TEST_DRIVE_REQUESTED",
                        "New test drive request",
                        event.getBuyerUsername() + " requested a test drive" + slot + ".", event);

                case "TEST_DRIVE_CONFIRMED" -> notify(event.getBuyerUsername(), "TEST_DRIVE_CONFIRMED",
                        "Test drive confirmed",
                        "Your test drive was confirmed" + slot + ".", event);

                case "TEST_DRIVE_REJECTED" -> notify(event.getBuyerUsername(), "TEST_DRIVE_REJECTED",
                        "Test drive declined",
                        "The seller declined your test drive request" + slot + ".", event);

                case "TEST_DRIVE_CANCELLED" -> {
                    notify(event.getBuyerUsername(), "TEST_DRIVE_CANCELLED", "Test drive cancelled",
                            "A test drive" + slot + " was cancelled.", event);
                    notify(event.getSellerUsername(), "TEST_DRIVE_CANCELLED", "Test drive cancelled",
                            "A test drive" + slot + " was cancelled.", event);
                }

                case "TEST_DRIVE_RESCHEDULED" -> {
                    notify(event.getBuyerUsername(), "TEST_DRIVE_RESCHEDULED", "Test drive rescheduled",
                            "Your test drive moved to" + slot + ".", event);
                    notify(event.getSellerUsername(), "TEST_DRIVE_RESCHEDULED", "Test drive rescheduled",
                            "A test drive moved to" + slot + ".", event);
                }

                default -> log.debug("Ignoring test-drive event type {}", event.getEventType());
            }

        } catch (Exception ex) {
            log.error("Could not handle test-drive event: {}", ex.getMessage());
        }
    }

    private void notify(String recipient, String type, String title, String message, TestDriveEvent event) {
        if (recipient == null) return;
        notificationService.create(recipient, type, title, message,
                String.valueOf(event.getTestDriveId()), "TEST_DRIVE");
    }
}