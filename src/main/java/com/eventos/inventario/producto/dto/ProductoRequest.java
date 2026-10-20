package com.eventos.inventario.producto.dto;

import com.eventos.inventario.producto.TipoProducto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ProductoRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        @Schema(description = "nombre del producto", example = "Vaso")
        String nombre,

        @Schema(description = "Tipo de producto", example = "CRISTALERIA")
        @NotNull(message = "El tipo es obligatorio")
        TipoProducto tipo
) {
}
