package com.eventos.inventario.compartido.storage;

import com.eventos.inventario.compartido.exceptions.InvalidFileException;
import com.eventos.inventario.compartido.exceptions.StorageException;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.io.IOException;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ImageStorage {

    private final FileStorage fileStorage;

    /** Valida que sea una imagen real y la guarda con un nombre generado por nosotros. */
    public String store(String folder, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new InvalidFileException("El archivo está vacío");
        }
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw new StorageException("No se pudo leer el archivo recibido", e);
        }
        ImageType type = ImageType.detect(content)
                .orElseThrow(() -> new InvalidFileException(
                        "Formato no soportado: solo se aceptan imágenes JPEG, PNG o WebP"));

        String filename = UUID.randomUUID() + "." + type.getExtension();
        return fileStorage.save(folder, filename, content);
    }

    public Resource load(String path) {
        return fileStorage.load(path);
    }

    public MediaType mediaType(String path) {
        return ImageType.mediaTypeOf(path);
    }

    public void delete(String path) {
        fileStorage.delete(path);
    }

    public StoredImage read(String path) {
        return new StoredImage(load(path), mediaType(path));
    }

    /** Borra el archivo solo si la transacción actual se confirma. */
    public void deleteWhenCommitted(String path) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_COMMITTED) {
                    delete(path);
                }
            }
        });
    }

    /** Borra el archivo solo si la transacción actual se revierte. */
    public void deleteWhenRolledBack(String path) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) {
                    delete(path);
                }
            }
        });
    }
}