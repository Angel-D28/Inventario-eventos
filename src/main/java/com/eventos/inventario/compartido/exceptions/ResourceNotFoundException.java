package com.eventos.inventario.compartido.exceptions;

import java.util.UUID;

public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message, UUID id) {
        super(message + "no encontrado" + id);
    }
}
