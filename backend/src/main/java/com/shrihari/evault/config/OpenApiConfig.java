package com.shrihari.evault.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    private static final String SECURITY_SCHEME_NAME = "bearerAuth";

    @Bean
    public OpenAPI evaultOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("SIH26190 — Secure Digital Evidence & Legal Document Management System")
                        .description("Chain-of-custody-grade backend platform for collecting, storing, analysing, and certifying digital evidence and legal documents under Bharatiya Sakshya Adhiniyam, 2023 (§63).")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("Smart India Hackathon Team")
                                .email("contact@evault.gov.in"))
                        .license(new License()
                                .name("Proprietary / SIH 2024")
                                .url("https://sih.gov.in")))
                .servers(List.of(
                        new Server().url("http://localhost:8943").description("Local Development Server")
                ))
                .addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_NAME))
                .components(new Components()
                        .addSecuritySchemes(SECURITY_SCHEME_NAME, new SecurityScheme()
                                .name(SECURITY_SCHEME_NAME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Enter JWT Bearer token")));
    }
}
