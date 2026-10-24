package com.carventure.authservice.client;

import com.carventure.authservice.dto.UserManagementUserDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "usermanagement")
public interface UserManagementClient {

    @PostMapping("/api/v1/user/registerCustomer")
    UserManagementUserDTO registerCustomer(
            @RequestBody UserManagementUserDTO user
    );

    @PostMapping("/api/v1/user/registerSeller")
    UserManagementUserDTO registerSeller(
            @RequestBody UserManagementUserDTO user
    );
}