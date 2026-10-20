package com.eventos.inventario.compartido.storage;

import lombok.Getter;
import org.springframework.http.MediaType;

import java.util.Optional;

@Getter
public enum ImageType {
    JPEG("image/jpeg", "jpg"),
    PNG("image/png", "png"),
    WEBP("image/webp", "webp");

    private final String mimeType;
    private final String extension;

    ImageType(String mimeType, String extension) {
        this.mimeType = mimeType;
        this.extension = extension;
    }

    /** Detecta el formato por los primeros bytes (firma), ignorando nombre y Content-Type. */
    public static Optional<ImageType> detect(byte[] b) {
        if (isJpeg(b)) return Optional.of(JPEG);
        if (isPng(b)) return Optional.of(PNG);
        if (isWebp(b)) return Optional.of(WEBP);
        return Optional.empty();
    }

    public static MediaType mediaTypeOf(String path) {
        String lower = path.toLowerCase();
        for (ImageType type : values()) {
            if (lower.endsWith("." + type.extension)) {
                return MediaType.parseMediaType(type.mimeType);
            }
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    private static boolean isJpeg(byte[] b) {
        return b.length >= 3
                && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF;
    }

    private static boolean isPng(byte[] b) {
        int[] signature = {0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        if (b.length < signature.length) return false;
        for (int i = 0; i < signature.length; i++) {
            if ((b[i] & 0xFF) != signature[i]) return false;
        }
        return true;
    }

    private static boolean isWebp(byte[] b) {
        return b.length >= 12
                && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P';
    }
}