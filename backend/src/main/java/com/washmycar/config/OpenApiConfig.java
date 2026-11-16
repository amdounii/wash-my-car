package com.washmycar.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Documentation Swagger UI : http://localhost:8081/swagger-ui.html
 * Bouton « Authorize » : coller le jeton obtenu via POST /api/auth/login.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "Wash My Car API", version = "1.0",
                description = "Recherche et réservation de stations de lavage en Tunisie. Prix en dinars tunisiens (TND)."),
        security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
