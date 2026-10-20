package com.eventos.inventario.planeador.dto;

import com.eventos.inventario.planeador.Planeador;

import java.util.UUID;

public record PlaneadorResponse(
        UUID id,
        String nombre,
        String telefono,
        String notas
) {
    public static PlaneadorResponse desde(Planeador p) {
        return new PlaneadorResponse(p.getId(), p.getNombre(), p.getTelefono(), p.getNotas());
    }
}