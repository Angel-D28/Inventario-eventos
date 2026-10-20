package com.eventos.inventario.planeador.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PlaneadorRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String nombre,

        @Size(max = 30, message = "El teléfono no puede superar 30 caracteres")
        String telefono,

        String notas
) {
}