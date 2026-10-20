package com.eventos.inventario.reporte;

import com.eventos.inventario.reporte.dto.ReporteEventoResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/eventos")
@RequiredArgsConstructor
@Tag(name = "Reportes", description = "Resumen del cuadre y la evidencia de un evento")
@SecurityRequirement(name = "bearerAuth")
public class ReporteController {

    private final ReporteService service;

    @Operation(summary = "Reporte del evento",
            description = "Totales por tipo de producto, detalle por producto, si el inventario cuadra "
                    + "y la lista de fotos de evidencia.")
    @ApiResponse(responseCode = "404", description = "No existe un evento con ese ID")
    @GetMapping("/{id}/reporte")
    public ReporteEventoResponse reporte(@PathVariable UUID id) {
        return service.generar(id);
    }
}