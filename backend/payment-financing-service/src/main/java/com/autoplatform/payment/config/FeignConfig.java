package com.autoplatform.payment.config;

import feign.RequestInterceptor;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Forwards the caller's bearer token to downstream Feign calls (Car Service),
 * mirroring scheduling-service's FeignConfig. Without this, CarServiceClient
 * calls go out with no Authorization header at all and Car Service correctly
 * rejects them with 401 - being authenticated on THIS service's incoming
 * request never automatically propagates to an outgoing Feign call.
 */

@Configuration
public class FeignConfig {

    @Value("${internal.api.key}")
    private String internalApiKey;

    @Bean
    public RequestInterceptor bearerTokenForwardingInterceptor() {
        return requestTemplate -> {
            requestTemplate.header("X-Internal-Key", internalApiKey);

            ServletRequestAttributes attributes =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes == null) {
                return;
            }
            HttpServletRequest request = attributes.getRequest();
            String authHeader = request.getHeader("Authorization");
            if (authHeader != null && !authHeader.isBlank()) {
                requestTemplate.header("Authorization", authHeader);
            }
        };
    }
}