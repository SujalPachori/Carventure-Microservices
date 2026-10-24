package com.carventure.offer.client;

import com.carventure.offer.dto.UserResponse;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class UserLookup {

    private static final Logger log = LoggerFactory.getLogger(UserLookup.class);

    private final UserClient userClient;

    public UserLookup(UserClient userClient) {
        this.userClient = userClient;
    }

    @CircuitBreaker(name = "userManagement", fallbackMethod = "getCustomerFallback")
    public UserResponse getCustomer(String customerId) {
        return userClient.getCustomer(customerId);
    }

    public UserResponse getCustomerFallback(String customerId, Throwable throwable) {
        log.warn("User management unavailable for {}", customerId);
        return null;
    }
}
