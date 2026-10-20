package com.eventos.inventario.compartido.storage;

import com.eventos.inventario.compartido.exceptions.StorageException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

@Component
public class LocalFileStorage implements FileStorage {

    private final Path root;

    public LocalFileStorage(@Value("${app.storage.root}") String root) {
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    @Override
    public String save(String folder, String filename, byte[] content) {
        Path target = resolve(folder + "/" + filename);
        try {
            Files.createDirectories(target.getParent());
            Files.write(target, content, StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new StorageException("No se pudo guardar el archivo", e);
        }
        return root.relativize(target).toString().replace('\\', '/');
    }

    @Override
    public Resource load(String path) {
        Path file = resolve(path);
        if (!Files.isRegularFile(file)) {
            throw new StorageException("Archivo no encontrado en el almacenamiento: " + path);
        }
        return new FileSystemResource(file);
    }

    @Override
    public void delete(String path) {
        try {
            Files.deleteIfExists(resolve(path));
        } catch (IOException e) {
            throw new StorageException("No se pudo borrar el archivo", e);
        }
    }

    /** Defensa en profundidad: ninguna ruta puede escapar de la carpeta raíz. */
    private Path resolve(String relative) {
        Path resolved = root.resolve(relative).normalize();
        if (!resolved.startsWith(root)) {
            throw new StorageException("Ruta fuera del almacenamiento: " + relative);
        }
        return resolved;
    }
}