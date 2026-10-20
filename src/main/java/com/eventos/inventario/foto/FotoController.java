package com.eventos.inventario.foto;

import com.eventos.inventario.compartido.storage.StoredImage;
import com.eventos.inventario.foto.dto.FotoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Fotos", description = "Evidencia fotográfica de recepción y devolución")
@SecurityRequirement(name = "bearerAuth")
public class FotoController {

    private final FotoService service;

    @Operation(summary = "Subir una foto de evidencia",
            description = "Formato multipart. Acepta JPEG, PNG o WebP de hasta 10 MB. "
                    + "'productoId' es opcional (foto de un producto concreto); sin él, la foto es general. "
                    + "'tomadaEn' es opcional, en formato ISO-8601 (por ejemplo 2026-10-19T18:30:00Z).")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Foto guardada"),
            @ApiResponse(responseCode = "400", description = "Archivo vacío o formato no soportado"),
            @ApiResponse(responseCode = "404", description = "El evento no existe"),
            @ApiResponse(responseCode = "409", description = "Estado del evento incorrecto o producto que no forma parte del evento"),
            @ApiResponse(responseCode = "413", description = "El archivo supera el tamaño máximo")
    })
    @PostMapping(path = "/eventos/{eventoId}/fotos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FotoResponse> subir(@PathVariable UUID eventoId,
                                              @RequestParam MomentoFoto momento,
                                              @RequestParam(required = false) UUID productoId,
                                              @RequestParam(required = false) Instant tomadaEn,
                                              @RequestPart("file") MultipartFile file) {
        FotoResponse creada = service.subir(eventoId, momento, productoId, tomadaEn, file);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/fotos/{id}/archivo").buildAndExpand(creada.id()).toUri();
        return ResponseEntity.created(ubicacion).body(creada);
    }

    @Operation(summary = "Listar las fotos de un evento, en orden cronológico")
    @ApiResponse(responseCode = "404", description = "El evento no existe")
    @GetMapping("/eventos/{eventoId}/fotos")
    public List<FotoResponse> listar(@PathVariable UUID eventoId) {
        return service.listar(eventoId);
    }

    @Operation(summary = "Descargar el archivo de una foto")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Imagen"),
            @ApiResponse(responseCode = "404", description = "La foto no existe")
    })
    @GetMapping("/fotos/{id}/archivo")
    public ResponseEntity<Resource> archivo(@PathVariable UUID id) {
        StoredImage archivo = service.archivo(id);
        return ResponseEntity.ok()
                .contentType(archivo.mediaType())
                .cacheControl(CacheControl.maxAge(Duration.ofDays(1)).cachePrivate())
                .body(archivo.resource());
    }

    @Operation(summary = "Eliminar una foto",
            description = "Solo mientras el evento no esté cerrado.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Foto eliminada"),
            @ApiResponse(responseCode = "404", description = "La foto no existe"),
            @ApiResponse(responseCode = "409", description = "El evento está cerrado")
    })
    @DeleteMapping("/fotos/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable UUID id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}