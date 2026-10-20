package com.eventos.inventario.evento;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EventoRepository extends JpaRepository<Evento, UUID> {
    @EntityGraph(attributePaths = {"planeador", "items", "items.producto"})
    Optional<Evento> findConDetalleById(UUID id);

    @EntityGraph(attributePaths = "planeador")
    List<Evento> findAllByOrderByFechaDesc();
}
