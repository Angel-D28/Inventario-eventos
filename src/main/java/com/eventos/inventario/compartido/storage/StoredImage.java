package com.eventos.inventario.compartido.storage;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;

public record StoredImage(Resource resource, MediaType mediaType) {
}
