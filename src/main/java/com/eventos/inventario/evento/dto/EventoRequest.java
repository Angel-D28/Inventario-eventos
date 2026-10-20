package com.eventos.inventario.evento.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record EventoRequest(
        @NotNull(message = "El planeador es obligatorio")
        UUID planeadorId,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 200, message = "El nombre no puede superar 200 caracteres")
        String nombre,

        @Size(max = 200, message = "El lugar no puede superar 200 caracteres")
        String lugar,

        @NotNull(message = "La fecha es obligatoria")
        LocalDate fecha
) {
}