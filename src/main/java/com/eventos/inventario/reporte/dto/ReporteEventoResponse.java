package com.eventos.inventario.reporte.dto;

import com.eventos.inventario.evento.EstadoEvento;
import com.eventos.inventario.evento.dto.EventoItemResponse;
import com.eventos.inventario.foto.dto.FotoResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record ReporteEventoResponse(
        UUID eventoId,
        String nombre,
        String lugar,
        LocalDate fecha,
        EstadoEvento estado,
        String planeador,
        boolean cuadra,
        List<TotalPorTipo> totalesPorTipo,
        List<EventoItemResponse> items,
        List<FotoResponse> fotos
) {
}