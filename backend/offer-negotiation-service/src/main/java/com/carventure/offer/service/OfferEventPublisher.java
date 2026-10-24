package com.carventure.offer.service;

import com.carventure.offer.entity.Offer;
import com.carventure.offer.event.OfferEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class OfferEventPublisher {

    private static final String TOPIC = "offer-events";

    private static final Logger log = LoggerFactory.getLogger(OfferEventPublisher.class);

    private final KafkaTemplate<String, OfferEvent> kafkaTemplate;

    public OfferEventPublisher(KafkaTemplate<String, OfferEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(String eventType, Offer offer, String recipientId) {

        OfferEvent event = new OfferEvent();
        event.setEventType(eventType);
        event.setOfferId(offer.getOfferId());
        event.setCarId(offer.getCarId());
        event.setSellerId(offer.getSellerId());
        event.setCustomerId(offer.getCustomerId());
        event.setRecipientId(recipientId);
        event.setOfferedAmount(offer.getOfferedAmount());
        event.setOfferedBy(offer.getOfferedBy());
        event.setOccurredAt(LocalDateTime.now());

        kafkaTemplate.send(TOPIC, offer.getCarId(), event)
                .whenComplete((result, error) -> {
                    if (error != null) {
                        log.error("Failed to publish {} for offer {}", eventType, offer.getOfferId(), error);
                    }
                });
    }
}
