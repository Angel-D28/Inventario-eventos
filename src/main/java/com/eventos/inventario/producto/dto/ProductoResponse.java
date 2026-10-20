package com.eventos.inventario.producto.dto;

import com.eventos.inventario.producto.Producto;
import com.eventos.inventario.producto.TipoProducto;

import java.util.UUID;

public record ProductoResponse(
        UUID id,
        String nombre,
        TipoProducto tipo,
        String fotoUrl,
        boolean activo
) {
    public static ProductoResponse desde(Producto p) {
        String fotoUrl = p.getFotoReferencia() == null
                ? null
                : "/api/productos/" + p.getId() + "/foto";
        return new ProductoResponse(p.getId(), p.getNombre(), p.getTipo(), fotoUrl, p.isActivo());
    }
}