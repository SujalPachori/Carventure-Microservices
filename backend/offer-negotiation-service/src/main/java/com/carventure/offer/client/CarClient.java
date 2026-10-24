package com.carventure.offer.client;

import com.carventure.offer.dto.CarResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "car")
public interface CarClient {

    @GetMapping("/api/v1/car/viewCar/{carId}")
    CarResponse getCar(@PathVariable String carId);

    @PutMapping("/api/v1/car/updateCarStatus/{carId}")
    void updateStatus(@PathVariable String carId, @RequestParam String status);
}
