package com.carventure.communication.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "usermanagement")
public interface UserServiceClient {

    @GetMapping("/api/v1/user/byId/{userId}")
    String getUsername(@PathVariable("userId") Integer userId);
}