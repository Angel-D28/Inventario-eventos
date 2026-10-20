package com.eventos.inventario.evento.dto;

import com.eventos.inventario.evento.EventoItem;
import com.eventos.inventario.producto.TipoProducto;

import java.util.UUID;

public record EventoItemResponse(
        UUID productoId,
        String productoNombre,
        TipoProducto tipo,
        int cantidadRecibida,
        int cantidadDevuelta,
        int cantidadRota,
        int cantidadPerdida,
        int cantidadConsumida,
        int diferencia,
        String observaciones
) {
    public static EventoItemResponse desde(EventoItem i) {
        return new EventoItemResponse(
                i.getProducto().getId(), i.getProducto().getNombre(), i.getProducto().getTipo(),
                i.getCantidadRecibida(), i.getCantidadDevuelta(), i.getCantidadRota(),
                i.getCantidadPerdida(), i.getCantidadConsumida(),
                i.diferencia(), i.getObservacionesRecepcion());
    }
}