package com.eventos.inventario.evento;

import java.util.UUID;

/** Lo que el módulo evento necesita saber sobre las fotos, sin depender del módulo foto. */
public interface EvidenciaFotografica {

    boolean hayFotosDeRecepcion(UUID eventoId);

    boolean hayFotosDeDevolucion(UUID eventoId);
}