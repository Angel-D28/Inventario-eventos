package com.eventos.inventario.reporte;

import com.eventos.inventario.evento.EventoService;
import com.eventos.inventario.evento.dto.EventoItemResponse;
import com.eventos.inventario.evento.dto.EventoResponse;
import com.eventos.inventario.foto.FotoService;
import com.eventos.inventario.foto.dto.FotoResponse;
import com.eventos.inventario.producto.TipoProducto;
import com.eventos.inventario.reporte.dto.ReporteEventoResponse;
import com.eventos.inventario.reporte.dto.TotalPorTipo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReporteService {

    private final EventoService eventoService;
    private final FotoService fotoService;

    public ReporteEventoResponse generar(UUID eventoId) {
        EventoResponse evento = eventoService.obtener(eventoId);
        List<FotoResponse> fotos = fotoService.listar(eventoId);

        Map<TipoProducto, List<EventoItemResponse>> porTipo = evento.items().stream()
                .collect(Collectors.groupingBy(
                        EventoItemResponse::tipo,
                        () -> new EnumMap<>(TipoProducto.class),
                        Collectors.toList()));

        List<TotalPorTipo> totales = porTipo.entrySet().stream()
                .map(e -> TotalPorTipo.desde(e.getKey(), e.getValue()))
                .toList();

        boolean cuadra = evento.items().stream().allMatch(i -> i.diferencia() == 0);

        return new ReporteEventoResponse(
                evento.id(), evento.nombre(), evento.lugar(), evento.fecha(),
                evento.estado(), evento.planeadorNombre(),
                cuadra, totales, evento.items(), fotos);
    }
}