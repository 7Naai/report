package com.pedidos360.report.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI reportOpenAPI() {

        return new OpenAPI()
                .info(
                        new Info()
                                .title("Pedidos360 - Reportería API")
                                .description(
                                        "API de reportería y analítica "
                                                + "del sistema Pedidos360."
                                )
                                .version("1.0.0")
                                .contact(
                                        new Contact()
                                                .name("Pedidos360")
                                )
                );
    }
}