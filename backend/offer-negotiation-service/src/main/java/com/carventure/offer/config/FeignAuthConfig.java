package com.carventure.offer.config;

import feign.RequestInterceptor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
public class FeignAuthConfig {

    // Must match car-service's `internal.api.key` (and any other downstream
    // service that trusts this header for service-to-service calls).
    @Value("${internal.api.key}")
    private String internalApiKey;

    @Bean
    public RequestInterceptor forwardAuthorizationHeader() {

        return template -> {

            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();

            // Calls made inside a user's HTTP request (e.g. from OfferController)
            // forward that user's JWT so downstream services see the real caller.
            if (attributes != null) {
                String header = attributes.getRequest().getHeader("Authorization");
                if (header != null && !header.isBlank()) {
                    template.header("Authorization", header);
                }
            }

            // Calls made off the request thread - e.g. ReservationExpiryJob's
            // @Scheduled sweep - have no Authorization header to forward at all.
            // Without this, those calls go out unauthenticated, get a 401 from
            // car-service, and silently trip the carInventory circuit breaker.
            template.header("X-Internal-Key", internalApiKey);
        };
    }
}