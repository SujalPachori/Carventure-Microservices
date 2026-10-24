package com.cardrive.scheduling.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    private static final String BEARER_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI schedulingServiceOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Scheduling Service API")
                        .description("""
                                Seller availability and test-drive booking lifecycle for the car
                                marketplace platform. Overlap safety for the same car is guaranteed
                                at the database level via a partial PostgreSQL exclusion constraint -
                                see the README for details.
                                """)
                        .version("v1")
                        .contact(new Contact().name("Car Marketplace Platform Team")))
                .addSecurityItem(new SecurityRequirement().addList(BEARER_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(BEARER_SCHEME, new SecurityScheme()
                                .name(BEARER_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT issued by the platform's identity service. Must carry userId and role claims.")));
    }
}
