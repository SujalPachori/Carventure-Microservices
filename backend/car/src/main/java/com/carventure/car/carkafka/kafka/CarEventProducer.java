package com.carventure.car.carkafka.kafka;

import com.carventure.car.carkafka.event.CarListedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes Car service domain events for the Reporting service to consume.
 * Call publishCarListed(...) from CarServiceImpl right after a successful addCar() save.
 */
@Component
@RequiredArgsConstructor
public class CarEventProducer {

    public static final String TOPIC_CAR_LISTED = "car.listed";

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public void publishCarListed(CarListedEvent event) {
        System.out.println("Car Listing Published");
        kafkaTemplate.send(TOPIC_CAR_LISTED, String.valueOf(event.getCarId()), event);
    }
}
