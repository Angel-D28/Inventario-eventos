package com.eventos.inventario.evento.dto;

import com.eventos.inventario.evento.EstadoEvento;
import com.eventos.inventario.evento.Evento;

import java.time.LocalDate;
import java.util.UUID;

public record EventoResumenResponse(
        UUID id,
        String nombre,
        String lugar,
        LocalDate fecha,
        EstadoEvento estado,
        UUID planeadorId,
        String planeadorNombre
) {
    public static EventoResumenResponse desde(Evento e) {
        return new EventoResumenResponse(
                e.getId(), e.getNombre(), e.getLugar(), e.getFecha(), e.getEstado(),
                e.getPlaneador().getId(), e.getPlaneador().getNombre());
    }
}