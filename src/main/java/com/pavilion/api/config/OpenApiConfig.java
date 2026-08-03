package com.pavilion.api.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI pavilionOpenApi() {
        String cookieScheme = "sessionCookie";
        return new OpenAPI()
                .info(new Info()
                        .title("Pavilion API")
                        .description("Java/Spring Boot backend for the Pavilion community app — auth, "
                                + "visitor entry OTPs, emergency alerts, and a Gemini-backed chat assistant.")
                        .version("v1"))
                .components(new Components()
                        .addSecuritySchemes(cookieScheme, new SecurityScheme()
                                .type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE)
                                .name("session")
                                .description("httpOnly JWT session cookie set by /api/auth/login or /api/auth/signup/verify")));
    }
}
