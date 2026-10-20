package com.eventos.inventario.foto;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface FotoRepository extends JpaRepository<Foto, UUID> {

    @EntityGraph(attributePaths = "producto")
    List<Foto> findByEventoIdOrderByTomadaEnAsc(UUID eventoId);

    boolean existsByEventoIdAndMomento(UUID eventoId, MomentoFoto momento);
}