package com.eventos.inventario.evento.dto;

import com.eventos.inventario.evento.EstadoEvento;
import com.eventos.inventario.evento.Evento;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record EventoResponse(
        UUID id,
        String nombre,
        String lugar,
        LocalDate fecha,
        EstadoEvento estado,
        UUID planeadorId,
        String planeadorNombre,
        List<EventoItemResponse> items
) {
    public static EventoResponse desde(Evento e) {
        List<EventoItemResponse> items = e.getItems().stream()
                .map(EventoItemResponse::desde)
                .sorted(Comparator.comparing(EventoItemResponse::productoNombre))
                .toList();
        return new EventoResponse(
                e.getId(), e.getNombre(), e.getLugar(), e.getFecha(), e.getEstado(),
                e.getPlaneador().getId(), e.getPlaneador().getNombre(), items);
    }
}