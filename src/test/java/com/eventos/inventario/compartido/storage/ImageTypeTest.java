package com.eventos.inventario.compartido.storage;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class ImageTypeTest {

    @Test
    void detectaJpeg() {
        byte[] jpeg = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16};
        assertThat(ImageType.detect(jpeg)).contains(ImageType.JPEG);
    }

    @Test
    void detectaPng() {
        byte[] png = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0};
        assertThat(ImageType.detect(png)).contains(ImageType.PNG);
    }

    @Test
    void detectaWebp() {
        byte[] webp = "RIFF....WEBPVP8 ".getBytes(StandardCharsets.US_ASCII);
        assertThat(ImageType.detect(webp)).contains(ImageType.WEBP);
    }

    @Test
    void rechazaUnTextoAunqueSePresenteComoImagen() {
        assertThat(ImageType.detect("esto no es una imagen".getBytes(StandardCharsets.UTF_8))).isEmpty();
        assertThat(ImageType.detect("<html><script>alert(1)</script>".getBytes(StandardCharsets.UTF_8))).isEmpty();
    }

    @Test
    void rechazaContenidoVacioOMuyCorto() {
        assertThat(ImageType.detect(new byte[0])).isEmpty();
        assertThat(ImageType.detect(new byte[]{(byte) 0xFF})).isEmpty();
    }

    @Test
    void elTipoDeContenidoSaleDeLaExtension() {
        assertThat(ImageType.mediaTypeOf("eventos/x/a.PNG")).isEqualTo(MediaType.IMAGE_PNG);
        assertThat(ImageType.mediaTypeOf("a.txt")).isEqualTo(MediaType.APPLICATION_OCTET_STREAM);
    }
}