package com.eventos.inventario.evento.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record EventoItemRequest(
        @NotNull(message = "La cantidad recibida es obligatoria")
        @Positive(message = "La cantidad recibida debe ser mayor que cero")
        Integer cantidadRecibida,

        String observaciones
) {
}