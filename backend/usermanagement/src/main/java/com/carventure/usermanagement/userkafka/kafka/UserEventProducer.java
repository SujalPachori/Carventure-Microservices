package com.carventure.usermanagement.userkafka.kafka;

import com.carventure.usermanagement.userkafka.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes User Management domain events for the Reporting service to consume.
 * Call publishUserRegistered(...) right after a new User (and its UserCredential) is saved.
 */
@Component
@RequiredArgsConstructor
public class UserEventProducer {

    public static final String TOPIC_USER_REGISTERED = "user.registered";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishUserRegistered(UserRegisteredEvent event) {
        kafkaTemplate.send(TOPIC_USER_REGISTERED, event.getUsername(), event);
    }
}
