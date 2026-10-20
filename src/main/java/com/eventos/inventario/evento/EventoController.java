package com.eventos.inventario.evento;

import com.eventos.inventario.evento.dto.EventoItemRequest;
import com.eventos.inventario.evento.dto.EventoItemResponse;
import com.eventos.inventario.evento.dto.EventoRequest;
import com.eventos.inventario.evento.dto.EventoResponse;
import com.eventos.inventario.evento.dto.EventoResumenResponse;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import com.eventos.inventario.evento.dto.DevolucionItemRequest;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/eventos")
@RequiredArgsConstructor
@Tag(name = "Eventos", description = "Ciclo de custodia: recepción, devolución y cuadre")
@SecurityRequirement(name = "bearerAuth")
public class EventoController {

    private final EventoService service;

    @Operation(summary = "Listar eventos, del más reciente al más antiguo")
    @GetMapping
    public List<EventoResumenResponse> listar() {
        return service.listar();
    }

    @Operation(summary = "Obtener un evento con sus productos")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evento encontrado"),
            @ApiResponse(responseCode = "404", description = "No existe un evento con ese ID")
    })
    @GetMapping("/{id}")
    public EventoResponse obtener(@PathVariable UUID id) {
        return service.obtener(id);
    }

    @Operation(summary = "Crear un evento",
            description = "El evento nace en estado PLANEADO.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Evento creado"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "El planeador no existe")
    })
    @PostMapping
    public ResponseEntity<EventoResponse> crear(@Valid @RequestBody EventoRequest request) {
        EventoResponse creado = service.crear(request);
        URI ubicacion = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(creado.id()).toUri();
        return ResponseEntity.created(ubicacion).body(creado);
    }

    @Operation(summary = "Registrar o actualizar un producto recibido",
            description = "Idempotente: si el producto ya está en el evento, actualiza su cantidad. "
                    + "Solo con el evento en estado PLANEADO.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Producto registrado"),
            @ApiResponse(responseCode = "404", description = "El evento o el producto no existen"),
            @ApiResponse(responseCode = "409", description = "El evento ya no está en estado PLANEADO, "
                    + "o el producto está desactivado")
    })
    @PutMapping("/{id}/items/{productoId}")
    public EventoItemResponse registrarItem(@PathVariable UUID id,
                                            @PathVariable UUID productoId,
                                            @Valid @RequestBody EventoItemRequest request) {
        return service.registrarItem(id, productoId, request);
    }

    @Operation(summary = "Quitar un producto del evento",
            description = "Solo con el evento en estado PLANEADO.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Producto quitado"),
            @ApiResponse(responseCode = "404", description = "El evento o el producto en el evento no existen"),
            @ApiResponse(responseCode = "409", description = "El evento ya no está en estado PLANEADO")
    })
    @DeleteMapping("/{id}/items/{productoId}")
    public ResponseEntity<Void> quitarItem(@PathVariable UUID id, @PathVariable UUID productoId) {
        service.quitarItem(id, productoId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "Confirmar la recepción del material",
            description = "Pasa el evento de PLANEADO a RECIBIDO. Requiere al menos un producto registrado.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evento recibido"),
            @ApiResponse(responseCode = "404", description = "No existe un evento con ese ID"),
            @ApiResponse(responseCode = "409", description = "El evento no está en PLANEADO o no tiene productos")
    })
    @PostMapping("/{id}/recibir")
    public EventoResponse recibir(@PathVariable UUID id) {
        return service.recibir(id);
    }

    @Operation(summary = "Registrar la devolución de un producto",
            description = "Idempotente: reemplaza las cuatro cantidades. Solo con el evento en estado RECIBIDO. "
                    + "La suma no puede superar lo recibido y solo los licores pueden tener consumidas.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Devolución registrada"),
            @ApiResponse(responseCode = "400", description = "Datos de entrada inválidos"),
            @ApiResponse(responseCode = "404", description = "El evento o el producto en el evento no existen"),
            @ApiResponse(responseCode = "409", description = "Estado incorrecto, suma excedida o consumida en un producto que no es licor")
    })
    @PutMapping("/{id}/items/{productoId}/devolucion")
    public EventoItemResponse registrarDevolucion(@PathVariable UUID id,
                                                  @PathVariable UUID productoId,
                                                  @Valid @RequestBody DevolucionItemRequest request) {
        return service.registrarDevolucion(id, productoId, request);
    }

    @Operation(summary = "Confirmar la devolución del material",
            description = "Pasa el evento de RECIBIDO a DEVUELTO. Exige que todos los productos cuadren.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evento devuelto"),
            @ApiResponse(responseCode = "404", description = "No existe un evento con ese ID"),
            @ApiResponse(responseCode = "409", description = "El evento no está en RECIBIDO o hay productos sin cuadrar")
    })
    @PostMapping("/{id}/devolver")
    public EventoResponse devolver(@PathVariable UUID id) {
        return service.devolver(id);
    }

    @Operation(summary = "Cerrar el evento",
            description = "Pasa el evento de DEVUELTO a CERRADO. Después de esto el evento no se modifica.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Evento cerrado"),
            @ApiResponse(responseCode = "404", description = "No existe un evento con ese ID"),
            @ApiResponse(responseCode = "409", description = "El evento no está en DEVUELTO")
    })
    @PostMapping("/{id}/cerrar")
    public EventoResponse cerrar(@PathVariable UUID id) {
        return service.cerrar(id);
    }
}