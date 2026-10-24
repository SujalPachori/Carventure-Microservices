package com.cardrive.scheduling.client;

import com.cardrive.scheduling.client.dto.CarDTO;
import com.cardrive.scheduling.exception.NotFoundException;
import com.cardrive.scheduling.exception.ValidationException;
import feign.FeignException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CarLookupService {

    private final CarDiscoveryClient carDiscoveryClient;
    public CarDTO getCarOrThrow(Long carId) {
        CarDTO car;
        try {
            car = carDiscoveryClient.getCar(carId);
        } catch (FeignException.NotFound e) {
            throw new NotFoundException("Car " + carId + " does not exist");
        } catch (FeignException e) {
            System.out.println(e);
            throw new ValidationException("Unable to verify car " + carId + " with Car Discovery Service");
        }
        if (car == null) {
            throw new NotFoundException("Car " + carId + " does not exist");
        }
        if (!car.active()) {
            throw new ValidationException("Car " + carId + " is not currently active for booking");
        }
        return car;
    }
}
