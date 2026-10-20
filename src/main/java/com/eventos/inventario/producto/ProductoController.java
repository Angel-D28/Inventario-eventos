package com.eventos.inventario.producto;

import com.eventos.inventario.producto.dto.ProductoRequest;
import com.eventos.inventario.producto.dto.ProductoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import com.eventos.inventario.compartido.storage.StoredImage;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/productos")
@RequiredArgsConstructor
@Tag(name = "Productos", description = "Catálogo genérico de licores, menaje y cristalería")
@SecurityRequirement(name = "bearerAuth")
public class ProductoController {

    private final ProductoService service;

    @Operation(summary = "Listar productos activos",
            description = "Devuelve los productos activos ordenados por nombre. Se puede filtrar por tipo.")
    @ApiResponse(responseCode = "200", description = "Lista obtenida")
    @GetMapping
    public List<ProductoResponse> listar(@RequestParam(required = false) TipoProducto tipo) {
        return service.listar(tipo);
    }

    @Operation(summary = "Obtener un producto por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto encontrado"),
            @ApiResponse(responseCode = "404", description = "No existe un producto con ese ID")
    })
    @GetMapping("/{id}")
    public ProductoResponse obtener(@PathVariable UUID id) {
        return service.obtener(id);
    }

    @Operation(summary = "Crear un producto",
            description = "Crea un producto en el catálogo y devuelve el producto creado.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Producto creado"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos")
    })
    @PostMapping
    public ResponseEntity<ProductoResponse> crear(@Valid @RequestBody ProductoRequest request) {
        ProductoResponse creado = service.crear(request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @Operation(summary = "Actualizar un producto",
            description = "Actualiza nombre y tipo, y devuelve el producto actualizado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "No existe un producto con ese ID")
    })
    @PutMapping("/{id}")
    public ProductoResponse actualizar(@PathVariable UUID id,
                                       @Valid @RequestBody ProductoRequest request) {
        return service.actualizar(id, request);
    }

    @Operation(summary = "Desactivar un producto",
            description = "Borrado lógico: el producto deja de aparecer en el catálogo, "
                    + "pero se conserva para el historial de eventos anteriores.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Producto desactivado"),
            @ApiResponse(responseCode = "404", description = "No existe un producto con ese ID")
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(@PathVariable UUID id) {
        service.desactivar(id);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Subir o reemplazar la foto de referencia",
            description = "Multipart con el campo 'file'. Acepta JPEG, PNG o WebP de hasta 10 MB.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Foto guardada"),
            @ApiResponse(responseCode = "400", description = "Archivo vacío o formato no soportado"),
            @ApiResponse(responseCode = "404", description = "No existe un producto con ese ID")
    })
    @PutMapping(path = "/{id}/foto", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProductoResponse subirFoto(@PathVariable UUID id,
                                      @RequestPart("file") MultipartFile file) {
        return service.subirFoto(id, file);
    }

    @Operation(summary = "Descargar la foto de referencia")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Imagen"),
            @ApiResponse(responseCode = "404", description = "El producto no existe o no tiene foto")
    })
    @GetMapping("/{id}/foto")
    public ResponseEntity<Resource> foto(@PathVariable UUID id) {
        StoredImage imagen = service.foto(id);
        return ResponseEntity.ok()
                .contentType(imagen.mediaType())
                .cacheControl(CacheControl.noCache().cachePrivate())
                .body(imagen.resource());
    }

    @Operation(summary = "Quitar la foto de referencia")
    @ApiResponse(responseCode = "404", description = "No existe un producto con ese ID")
    @DeleteMapping("/{id}/foto")
    public ProductoResponse quitarFoto(@PathVariable UUID id) {
        return service.quitarFoto(id);
    }
}