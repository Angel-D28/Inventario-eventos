package com.eventos.inventario.compartido.storage;

import com.eventos.inventario.compartido.exceptions.StorageException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalFileStorageTest {

    @TempDir
    Path raiz;

    LocalFileStorage storage;

    @BeforeEach
    void crear() {
        storage = new LocalFileStorage(raiz.toString());
    }

    @Test
    void guardaElArchivoYDevuelveUnaRutaRelativa() throws IOException {
        String ruta = storage.save("eventos/abc", "foto.png", new byte[]{1, 2, 3});

        assertThat(ruta).isEqualTo("eventos/abc/foto.png");
        assertThat(Files.readAllBytes(raiz.resolve(ruta))).containsExactly(1, 2, 3);
    }

    @Test
    void nuncaSobrescribeUnArchivoExistente() {
        storage.save("eventos/abc", "foto.png", new byte[]{1});

        assertThatThrownBy(() -> storage.save("eventos/abc", "foto.png", new byte[]{2}))
                .isInstanceOf(StorageException.class);
    }

    @Test
    void rechazaRutasQueEscapanDeLaCarpetaRaiz() {
        assertThatThrownBy(() -> storage.save("../fuera", "x.png", new byte[]{1}))
                .isInstanceOf(StorageException.class);
        assertThatThrownBy(() -> storage.load("../../etc/passwd"))
                .isInstanceOf(StorageException.class);
        assertThatThrownBy(() -> storage.delete("../otra-carpeta/x.png"))
                .isInstanceOf(StorageException.class);
    }

    @Test
    void borrarUnArchivoInexistenteNoFalla() {
        assertThatCode(() -> storage.delete("eventos/abc/no-existe.png")).doesNotThrowAnyException();
    }
}