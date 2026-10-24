package com.carventure.car.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI carventureOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("CarVenture API")
                        .description("CarVenture car marketplace services")
                        .version("v1.0"));
    }
}
