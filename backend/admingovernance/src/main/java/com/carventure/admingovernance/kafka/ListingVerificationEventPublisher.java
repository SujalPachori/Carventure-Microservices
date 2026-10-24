package com.carventure.admingovernance.kafka;

import com.carventure.admingovernance.kafka.events.ListingVerifiedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ListingVerificationEventPublisher {

    private static final String TOPIC = "listing.verified";

    private final KafkaTemplate<String, ListingVerifiedEvent> kafkaTemplate;

    public void publish(Long carId, String decision) {
        kafkaTemplate.send(TOPIC, String.valueOf(carId), new ListingVerifiedEvent(carId, decision));
    }
}