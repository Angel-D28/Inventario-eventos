package com.eventos.inventario.compartido.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "API de Inventario para Eventos",
        version = "0.0.1",
        description = "Control de inventario en custodia: recepción, devolución y cuadre por evento"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "basic", bearerFormat = "JWT")
public class OpenApiConfig {
}