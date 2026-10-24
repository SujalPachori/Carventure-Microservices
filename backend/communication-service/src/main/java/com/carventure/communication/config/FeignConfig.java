package com.carventure.communication.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Every outbound call from this service originates in a Kafka listener, so
 * there is never an incoming request whose bearer token we could forward.
 * The shared internal key is what authenticates us to usermanagement instead.
 */
@Configuration
public class FeignConfig {

    @Value("${internal.api.key}")
    private String internalApiKey;

    @Bean
    public RequestInterceptor internalKeyInterceptor() {
        return requestTemplate -> requestTemplate.header("X-Internal-Key", internalApiKey);
    }
}