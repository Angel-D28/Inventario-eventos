package com.eventos.inventario.reporte.dto;

import com.eventos.inventario.evento.dto.EventoItemResponse;
import com.eventos.inventario.producto.TipoProducto;

import java.util.List;

public record TotalPorTipo(
        TipoProducto tipo,
        int recibida,
        int devuelta,
        int rota,
        int perdida,
        int consumida,
        int diferencia
) {
    public static TotalPorTipo desde(TipoProducto tipo, List<EventoItemResponse> items) {
        return new TotalPorTipo(
                tipo,
                items.stream().mapToInt(EventoItemResponse::cantidadRecibida).sum(),
                items.stream().mapToInt(EventoItemResponse::cantidadDevuelta).sum(),
                items.stream().mapToInt(EventoItemResponse::cantidadRota).sum(),
                items.stream().mapToInt(EventoItemResponse::cantidadPerdida).sum(),
                items.stream().mapToInt(EventoItemResponse::cantidadConsumida).sum(),
                items.stream().mapToInt(EventoItemResponse::diferencia).sum());
    }
}