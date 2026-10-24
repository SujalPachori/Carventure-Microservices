package com.carventure.engagement.client;

import com.carventure.engagement.dto.CarResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class CarLookup {

    private static final Logger log = LoggerFactory.getLogger(CarLookup.class);

    private final CarClient carClient;

    public CarLookup(CarClient carClient) {
        this.carClient = carClient;
    }

    @CircuitBreaker(name = "carInventory", fallbackMethod = "getCarFallback")
    public CarResponse getCar(String carId) {
        return carClient.getCar(carId);
    }

    public CarResponse getCarFallback(String carId, Throwable throwable) {
        log.warn("Car inventory unavailable for {}", carId);
        return null;
    }
}
