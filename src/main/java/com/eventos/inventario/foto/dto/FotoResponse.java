package com.eventos.inventario.foto.dto;

import com.eventos.inventario.foto.Foto;
import com.eventos.inventario.foto.MomentoFoto;
import com.eventos.inventario.producto.Producto;

import java.time.Instant;
import java.util.UUID;

public record FotoResponse(
        UUID id,
        UUID eventoId,
        UUID productoId,
        String productoNombre,
        MomentoFoto momento,
        Instant tomadaEn,
        Instant subidaEn,
        String url
) {
    public static FotoResponse desde(Foto f) {
        Producto p = f.getProducto();
        return new FotoResponse(
                f.getId(), f.getEvento().getId(),
                p != null ? p.getId() : null,
                p != null ? p.getNombre() : null,
                f.getMomento(), f.getTomadaEn(), f.getCreatedAt(),
                "/api/fotos/" + f.getId() + "/archivo");
    }
}