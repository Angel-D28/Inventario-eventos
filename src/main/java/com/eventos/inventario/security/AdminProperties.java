package com.eventos.inventario.security;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "app.security.admin")
public record AdminProperties(
        @NotBlank String username,
        @NotBlank @Size(min = 12, message = "la contraseña debe tener al menos 12 caracteres") String password
) {
}