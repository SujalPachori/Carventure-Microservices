package com.autoplatform.payment.client;

import com.autoplatform.payment.exception.CarServiceUnavailableException;
import com.autoplatform.payment.exception.InvalidRequestException;
import com.autoplatform.payment.exception.ResourceNotFoundException;
import feign.FeignException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * The single place that validates a carId/sellerId pair against the real
 * Car Service before this service trusts them - mirrors the same gap that
 * was fixed in the Scheduling Service (SchedulingResource was previously
 * populated from client-supplied data with nothing to check it against).
 *
 * A plain 404 from Car Service ("car doesn't exist") is a normal business
 * outcome, not an infrastructure failure, so it's excluded from the circuit
 * breaker / retry policy via resilience4j.*.ignore-exceptions in
 * application.yml and handled directly here instead.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CarValidationService {

    private final CarServiceClient carServiceClient;

    public CarResponse getCarOrThrow(Long carId) {
        try {
            return fetchWithResilience(carId);
        } catch (FeignException.NotFound e) {
            throw new ResourceNotFoundException("Car " + carId + " does not exist in Car Service");
        }
    }

    @CircuitBreaker(name = "carService", fallbackMethod = "fetchFallback")
    @Retry(name = "carService")
    protected CarResponse fetchWithResilience(Long carId) {
        return carServiceClient.getCar(carId);
    }

    @SuppressWarnings("unused") // invoked reflectively by resilience4j
    private CarResponse fetchFallback(Long carId, Throwable throwable) {
        if (throwable instanceof FeignException.NotFound notFound) {
            throw notFound;
        }
        log.error("Car Service unavailable while fetching carId={}: {}", carId, throwable.getMessage(), throwable);
        throw new CarServiceUnavailableException("Car Service is currently unavailable; try again shortly");
    }

    /** Fetches the car and confirms it's listed by the given seller and still available for sale. */
    public CarResponse validateAvailableForPurchase(Long carId, Long expectedSellerId) {
        CarResponse car = getCarOrThrow(carId);

        if (!car.sellerId().equals(expectedSellerId)) {
            throw new InvalidRequestException("Car " + carId + " does not belong to seller " + expectedSellerId);
        }
        if (!car.listed()) {
            throw new InvalidRequestException("Car " + carId + " is no longer listed for sale");
        }
        return car;
    }
}
