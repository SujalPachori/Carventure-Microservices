package com.carventure.engagement.client;

import com.carventure.engagement.dto.CarResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "car")
public interface CarClient {

    @GetMapping("/api/v1/car/viewCar/{carId}")
    CarResponse getCar(@PathVariable String carId);
}
