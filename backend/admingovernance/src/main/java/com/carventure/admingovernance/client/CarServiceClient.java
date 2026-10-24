package com.carventure.admingovernance.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "car")
public interface CarServiceClient {

    @PutMapping("/updateCarStatus/{carId}")
    void updateCarStatus(@PathVariable("carId") Long carId, @RequestParam("status") String status);
}