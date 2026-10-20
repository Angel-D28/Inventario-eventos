package com.eventos.inventario.foto;

import com.eventos.inventario.evento.EvidenciaFotografica;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
class FotoEvidencia implements EvidenciaFotografica {

    private final FotoRepository repository;

    @Override
    public boolean hayFotosDeRecepcion(UUID eventoId) {
        return repository.existsByEventoIdAndMomento(eventoId, MomentoFoto.RECEPCION);
    }

    @Override
    public boolean hayFotosDeDevolucion(UUID eventoId) {
        return repository.existsByEventoIdAndMomento(eventoId, MomentoFoto.DEVOLUCION);
    }
}