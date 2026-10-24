package com.carventure.offer.client;

import com.carventure.offer.dto.CarResponse;
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

    @CircuitBreaker(name = "carInventory", fallbackMethod = "markReservedFallback")
    public void markReserved(String carId) {
        carClient.updateStatus(carId, "RESERVED");
    }

    public void markReservedFallback(String carId, Throwable throwable) {
        log.warn("Could not mark car {} as reserved", carId);
    }

    @CircuitBreaker(name = "carInventory", fallbackMethod = "markListedFallback")
    public void markListed(String carId) {
        carClient.updateStatus(carId, "ACTIVE");
    }

    public void markListedFallback(String carId, Throwable throwable) {
        log.warn("Could not release car {} back to listed", carId);
    }
}
