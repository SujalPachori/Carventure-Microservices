package com.carventure.communication.service;

import com.carventure.communication.client.UserServiceClient;
import com.carventure.communication.event.CarSoldEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class CarSoldListener {

    private static final Logger log = LoggerFactory.getLogger(CarSoldListener.class);

    private final NotificationService notificationService;
    private final UserServiceClient userServiceClient;

    public CarSoldListener(NotificationService notificationService, UserServiceClient userServiceClient) {
        this.notificationService = notificationService;
        this.userServiceClient = userServiceClient;
    }

    @KafkaListener(topics = "car-sold-events",
            groupId = "communication-carsold-group",
            containerFactory = "carSoldListenerContainerFactory")
    public void onCarSold(CarSoldEvent event) {

        String amount = event.getCurrency() + " " + event.getAmount().stripTrailingZeros().toPlainString();
        String reference = String.valueOf(event.getOrderId());

        notifySeller(event, amount, reference);
        notifyBuyer(event, amount, reference);
    }

    private void notifySeller(CarSoldEvent event, String amount, String reference) {
        try {
            String seller = userServiceClient.getUsername(event.getSellerId().intValue());
            notificationService.create(seller, "CAR_SOLD", "Your car sold",
                    "Payment of " + amount + " completed. The sale is confirmed.",
                    reference, "ORDER");
        } catch (Exception ex) {
            log.error("Could not notify seller for orderId={}: {}", event.getOrderId(), ex.getMessage());
        }
    }

    private void notifyBuyer(CarSoldEvent event, String amount, String reference) {
        try {
            String buyer = userServiceClient.getUsername(event.getBuyerId().intValue());
            notificationService.create(buyer, "PAYMENT_COMPLETED", "Payment complete",
                    "Your payment of " + amount + " went through. The car is yours.",
                    reference, "ORDER");
        } catch (Exception ex) {
            log.error("Could not notify buyer for orderId={}: {}", event.getOrderId(), ex.getMessage());
        }
    }
}