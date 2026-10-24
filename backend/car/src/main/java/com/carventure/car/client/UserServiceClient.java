package com.carventure.car.client;

import com.carventure.car.dto.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "usermanagement")
public interface UserServiceClient {

    @GetMapping("/api/v1/user/viewCustomer/{username}")
    UserResponse getCustomer(@PathVariable("username") String username);

    @GetMapping("/api/v1/user/viewSeller/{username}")
    UserResponse getSeller(@PathVariable("username") String username);

    @GetMapping("/api/v1/user/exists/{username}")
    boolean userExists(@PathVariable("username") String username);
}