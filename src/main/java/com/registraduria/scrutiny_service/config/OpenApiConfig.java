package com.registraduria.scrutiny_service.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI scrutinyOpenApi() {

        return new OpenAPI()
                .info(new Info()
                        .title("Scrutiny Service API")
                        .description("""
                                Electoral scrutiny microservice responsible
                                for E14 publication and consolidation.
                                """)
                        .version("v1.0")
                )
                .externalDocs(new ExternalDocumentation()
                        .description("Registraduria System"));
    }
}