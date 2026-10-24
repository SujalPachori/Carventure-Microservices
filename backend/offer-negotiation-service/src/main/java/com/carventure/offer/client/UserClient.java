package com.carventure.offer.client;

import com.carventure.offer.dto.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "usermanagement")
public interface UserClient {

    @GetMapping("/api/v1/user/viewCustomer/{customerId}")
    UserResponse getCustomer(@PathVariable String customerId);
}
