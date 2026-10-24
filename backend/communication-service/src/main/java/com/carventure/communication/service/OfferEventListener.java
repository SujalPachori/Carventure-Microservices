package com.carventure.communication.service;

import com.carventure.communication.event.OfferEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.text.NumberFormat;
import java.util.Locale;

@Service
public class OfferEventListener {

    private static final Logger log = LoggerFactory.getLogger(OfferEventListener.class);

    private final NotificationService notificationService;

    public OfferEventListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @KafkaListener(topics = "offer-events",
            groupId = "communication-offer-group",
            containerFactory = "offerListenerContainerFactory")
    public void onOfferEvent(OfferEvent event) {

        if (event.getRecipientId() == null) {
            log.warn("Offer event {} has no recipient", event.getOfferId());
            return;
        }

        String amount = format(event.getOfferedAmount());

        String title;
        String message;

        switch (event.getEventType()) {
            case "OFFER_RECEIVED" -> {
                title = "New offer received";
                message = "You received an offer of " + amount;
            }
            case "OFFER_COUNTERED" -> {
                title = "Counter offer";
                message = "You received a counter offer of " + amount;
            }
            case "OFFER_ACCEPTED" -> {
                title = "Offer accepted";
                message = "Your offer of " + amount + " was accepted";
            }
            case "OFFER_REJECTED" -> {
                title = "Offer rejected";
                message = "Your offer of " + amount + " was rejected";
            }
            case "OFFER_CLOSED" -> {
                title = "Car no longer available";
                message = "The seller accepted another offer on this car";
            }
            default -> {
                log.warn("Unknown offer event type {}", event.getEventType());
                return;
            }
        }

        notificationService.create(event.getRecipientId(), event.getEventType(),
                title, message, event.getOfferId(), "OFFER");
    }

    private String format(Double amount) {

        if (amount == null) {
            return "";
        }

        return NumberFormat.getCurrencyInstance(Locale.of("en", "IN")).format(amount);
    }
}
