package com.autoplatform.payment.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "car")
public interface CarServiceClient {

    @GetMapping("/api/v1/car/viewCarPayments/{carId}")
    CarResponse getCar(@PathVariable("carId") Long carId);

    @PutMapping("/api/v1/car/updateCarStatus/{carId}")
    void updateStatus(@PathVariable("carId") Long carId, @RequestParam("status") String status);
}
