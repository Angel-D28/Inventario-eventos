package com.eventos.inventario.compartido.storage;

import org.springframework.core.io.Resource;

public interface FileStorage {

    /** Guarda el contenido y devuelve la ruta relativa con la que se recupera. */
    String save(String folder, String filename, byte[] content);

    Resource load(String path);

    void delete(String path);
}