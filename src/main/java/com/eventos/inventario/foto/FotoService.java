package com.eventos.inventario.foto;

import com.eventos.inventario.compartido.exceptions.BusinessRuleException;
import com.eventos.inventario.compartido.exceptions.ResourceNotFoundException;
import com.eventos.inventario.compartido.storage.ImageStorage;
import com.eventos.inventario.compartido.storage.StoredImage;
import com.eventos.inventario.evento.EstadoEvento;
import com.eventos.inventario.evento.Evento;
import com.eventos.inventario.evento.EventoService;
import com.eventos.inventario.foto.dto.FotoResponse;
import com.eventos.inventario.producto.Producto;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FotoService {

    /** En qué estados del evento se acepta cada tipo de foto. CERRADO no acepta ninguna. */
    private static final Map<MomentoFoto, Set<EstadoEvento>> ESTADOS_PERMITIDOS = Map.of(
            MomentoFoto.RECEPCION, EnumSet.of(EstadoEvento.PLANEADO, EstadoEvento.RECIBIDO),
            MomentoFoto.DEVOLUCION, EnumSet.of(EstadoEvento.RECIBIDO, EstadoEvento.DEVUELTO));

    private final FotoRepository repository;
    private final EventoService eventoService;
    private final ImageStorage imageStorage;

    public List<FotoResponse> listar(UUID eventoId) {
        eventoService.obtenerEntidad(eventoId); // 404 si el evento no existe
        return repository.findByEventoIdOrderByTomadaEnAsc(eventoId).stream()
                .map(FotoResponse::desde)
                .toList();
    }

    @Transactional
    public FotoResponse subir(UUID eventoId, MomentoFoto momento, UUID productoId,
                              Instant tomadaEn, MultipartFile archivo) {
        Evento evento = eventoService.obtenerEntidad(eventoId);

        if (!ESTADOS_PERMITIDOS.get(momento).contains(evento.getEstado())) {
            throw new BusinessRuleException("No se pueden agregar fotos de " + momento
                    + " con el evento en estado " + evento.getEstado());
        }

        Producto producto = null;
        if (productoId != null) {
            producto = evento.buscarItem(productoId)
                    .orElseThrow(() -> new BusinessRuleException(
                            "El producto no forma parte de este evento"))
                    .getProducto();
        }

        String ruta = imageStorage.store("eventos/" + eventoId, archivo);
        imageStorage.deleteWhenRolledBack(ruta);

        Foto foto = new Foto(evento, producto, momento, ruta,
                tomadaEn != null ? tomadaEn : Instant.now());
        return FotoResponse.desde(repository.save(foto));
    }

    public StoredImage archivo(UUID id) {
        return imageStorage.read(buscar(id).getRuta());
    }

    @Transactional
    public void eliminar(UUID id) {
        Foto foto = buscar(id);
        if (foto.getEvento().getEstado() == EstadoEvento.CERRADO) {
            throw new BusinessRuleException(
                    "No se pueden eliminar fotos de un evento cerrado");
        }
        String ruta = foto.getRuta();
        repository.delete(foto);

        // El archivo solo se borra si la transacción realmente se confirmó.
        imageStorage.deleteWhenCommitted(ruta);
    }


    private Foto buscar(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Foto", id));
    }
}