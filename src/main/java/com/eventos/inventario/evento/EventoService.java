package com.eventos.inventario.evento;

import com.eventos.inventario.compartido.exceptions.BusinessRuleException;
import com.eventos.inventario.compartido.exceptions.ResourceNotFoundException;
import com.eventos.inventario.evento.dto.EventoItemRequest;
import com.eventos.inventario.evento.dto.EventoItemResponse;
import com.eventos.inventario.evento.dto.EventoRequest;
import com.eventos.inventario.evento.dto.EventoResponse;
import com.eventos.inventario.evento.dto.EventoResumenResponse;
import com.eventos.inventario.planeador.Planeador;
import com.eventos.inventario.planeador.PlaneadorService;
import com.eventos.inventario.producto.Producto;
import com.eventos.inventario.producto.ProductoService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.eventos.inventario.evento.dto.DevolucionItemRequest;
import com.eventos.inventario.producto.TipoProducto;
import java.util.stream.Collectors;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventoService {

    private final EventoRepository repository;
    private final PlaneadorService planeadorService;
    private final ProductoService productoService;
    private final EvidenciaFotografica evidencia;

    @Value("${app.evidencia.obligatoria:true}")
    private boolean evidenciaObligatoria;

    public List<EventoResumenResponse> listar() {
        return repository.findAllByOrderByFechaDesc().stream()
                .map(EventoResumenResponse::desde)
                .toList();
    }

    public EventoResponse obtener(UUID id) {
        return EventoResponse.desde(buscar(id));
    }

    @Transactional
    public EventoResponse crear(EventoRequest request) {
        Planeador planeador = planeadorService.obtenerEntidad(request.planeadorId());
        Evento evento = new Evento(planeador, request.nombre().trim(),
                request.lugar(), request.fecha());
        return EventoResponse.desde(repository.save(evento));
    }

    /** Agrega el producto al evento o, si ya está, actualiza su cantidad. */
    @Transactional
    public EventoItemResponse registrarItem(UUID eventoId, UUID productoId,
                                            EventoItemRequest request) {
        Evento evento = buscar(eventoId);
        exigirEstado(evento, EstadoEvento.PLANEADO);

        Producto producto = productoService.obtenerEntidad(productoId);
        if (!producto.isActivo()) {
            throw new BusinessRuleException(
                    "El producto '" + producto.getNombre() + "' está desactivado");
        }

        EventoItem item = evento.buscarItem(productoId)
                .orElseGet(() -> evento.agregarItem(producto));
        item.setCantidadRecibida(request.cantidadRecibida());
        item.setObservacionesRecepcion(request.observaciones());
        return EventoItemResponse.desde(item);
    }

    @Transactional
    public void quitarItem(UUID eventoId, UUID productoId) {
        Evento evento = buscar(eventoId);
        exigirEstado(evento, EstadoEvento.PLANEADO);

        EventoItem item = evento.buscarItem(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto en el evento", productoId));
        evento.quitarItem(item);
    }

    @Transactional
    public EventoResponse recibir(UUID id) {
        Evento evento = buscar(id);
        exigirEstado(evento, EstadoEvento.PLANEADO);
        if (evento.getItems().isEmpty()) {
            throw new BusinessRuleException(
                    "No se puede recibir un evento sin productos registrados");
        }
        if (evidenciaObligatoria && !evidencia.hayFotosDeRecepcion(id)) {
            throw new BusinessRuleException(
                    "Falta evidencia: sube al menos una foto de RECEPCION antes de confirmar la recepción");
        }
        evento.setEstado(EstadoEvento.RECIBIDO);
        return EventoResponse.desde(evento);
    }

    private Evento buscar(UUID id) {
        return repository.findConDetalleById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Evento", id));
    }

    private void exigirEstado(Evento evento, EstadoEvento requerido) {
        if (evento.getEstado() != requerido) {
            throw new BusinessRuleException(
                    "Operación no permitida: el evento está en estado "
                            + evento.getEstado() + " y se requiere " + requerido);
        }
    }
    @Transactional
    public EventoItemResponse registrarDevolucion(UUID eventoId, UUID productoId,
                                                  DevolucionItemRequest request) {
        Evento evento = buscar(eventoId);
        exigirEstado(evento, EstadoEvento.RECIBIDO);

        EventoItem item = evento.buscarItem(productoId)
                .orElseThrow(() -> new ResourceNotFoundException("Producto en el evento", productoId));

        Producto producto = item.getProducto();
        if (request.cantidadConsumida() > 0 && producto.getTipo() != TipoProducto.LICOR) {
            throw new BusinessRuleException(
                    "Solo los licores pueden registrarse como consumidos; '"
                            + producto.getNombre() + "' es " + producto.getTipo());
        }

        int total = request.cantidadDevuelta() + request.cantidadRota()
                + request.cantidadPerdida() + request.cantidadConsumida();
        if (total > item.getCantidadRecibida()) {
            throw new BusinessRuleException(
                    "Las cantidades suman " + total + " pero de '" + producto.getNombre()
                            + "' solo se recibieron " + item.getCantidadRecibida());
        }

        item.registrarDevolucion(request.cantidadDevuelta(), request.cantidadRota(),
                request.cantidadPerdida(), request.cantidadConsumida());
        return EventoItemResponse.desde(item);
    }

    @Transactional
    public EventoResponse devolver(UUID id) {
        Evento evento = buscar(id);
        exigirEstado(evento, EstadoEvento.RECIBIDO);

        List<EventoItem> sinCuadrar = evento.itemsSinCuadrar();
        if (!sinCuadrar.isEmpty()) {
            String detalle = sinCuadrar.stream()
                    .map(i -> i.getProducto().getNombre() + " (sin justificar: " + i.diferencia() + ")")
                    .collect(Collectors.joining(", "));
            throw new BusinessRuleException("El inventario no cuadra: " + detalle);
        }
        if (evidenciaObligatoria && !evidencia.hayFotosDeDevolucion(id)) {
            throw new BusinessRuleException(
                    "Falta evidencia: sube al menos una foto de DEVOLUCION antes de confirmar la devolución");
        }
        evento.setEstado(EstadoEvento.DEVUELTO);
        return EventoResponse.desde(evento);
    }

    @Transactional
    public EventoResponse cerrar(UUID id) {
        Evento evento = buscar(id);
        exigirEstado(evento, EstadoEvento.DEVUELTO);
        evento.setEstado(EstadoEvento.CERRADO);
        return EventoResponse.desde(evento);
    }

    /** Uso interno entre módulos: devuelve la entidad, no un DTO. */
    public Evento obtenerEntidad(UUID id) {
        return buscar(id);
    }
}