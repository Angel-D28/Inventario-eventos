package com.eventos.inventario.evento.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record DevolucionItemRequest(
        @NotNull(message = "La cantidad devuelta es obligatoria")
        @PositiveOrZero(message = "La cantidad devuelta no puede ser negativa")
        Integer cantidadDevuelta,

        @NotNull(message = "La cantidad rota es obligatoria")
        @PositiveOrZero(message = "La cantidad rota no puede ser negativa")
        Integer cantidadRota,

        @NotNull(message = "La cantidad perdida es obligatoria")
        @PositiveOrZero(message = "La cantidad perdida no puede ser negativa")
        Integer cantidadPerdida,

        @NotNull(message = "La cantidad consumida es obligatoria")
        @PositiveOrZero(message = "La cantidad consumida no puede ser negativa")
        Integer cantidadConsumida
) {
}