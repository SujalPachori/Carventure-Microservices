package com.cardrive.scheduling.client;

import com.cardrive.scheduling.client.dto.CarDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Client for the separate Car Discovery Service, which is the source of truth
 * for car existence and car -> seller ownership. The Scheduling Service never
 * trusts a client-supplied sellerId; it always resolves ownership through here.
 */
@FeignClient(name = "car")
public interface CarDiscoveryClient {

    @GetMapping("/api/v1/car/viewCarSched/{carId}")
    CarDTO getCar(@PathVariable("carId") Long carId);
}
