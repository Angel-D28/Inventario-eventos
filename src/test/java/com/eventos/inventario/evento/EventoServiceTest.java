package com.eventos.inventario.evento;

import com.eventos.inventario.compartido.exceptions.BusinessRuleException;
import com.eventos.inventario.evento.dto.DevolucionItemRequest;
import com.eventos.inventario.evento.dto.EventoItemRequest;
import com.eventos.inventario.evento.dto.EventoItemResponse;
import com.eventos.inventario.evento.dto.EventoResponse;
import com.eventos.inventario.planeador.Planeador;
import com.eventos.inventario.planeador.PlaneadorService;
import com.eventos.inventario.producto.Producto;
import com.eventos.inventario.producto.ProductoService;
import com.eventos.inventario.producto.TipoProducto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class EventoServiceTest {

    @Mock
    EventoRepository eventoRepository;
    @Mock
    PlaneadorService planeadorService;
    @Mock
    ProductoService productoService;
    @Mock
    EvidenciaFotografica evidenciaFotografica;
    //InjectMocks construye el servicio con los mocks por tipo, así que el orden de los campos en la clase no importan
    @InjectMocks EventoService eventoService;


    //El flag evidenciaObligatoria lo ponemos a mano, porque el @Value solo lo rellena Spring.
    @BeforeEach
    void activarEvidenciaObligatoria(){
        ReflectionTestUtils.setField(eventoService, "evidenciaObligatoria", true);
    }

    //Recibir test

    @Test
    void recibirSinProductosFalla(){
        Evento evento = eventoEn(EstadoEvento.PLANEADO);

        assertThatThrownBy(() -> eventoService.recibir(evento.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("sin productos");
    }

    @Test
    void recibirSinFotoDeRecepcionFalla() {
        Evento evento = eventoEn(EstadoEvento.PLANEADO);
        conItem(evento, producto("Copa", TipoProducto.CRISTALERIA), 10);
        when(evidenciaFotografica.hayFotosDeRecepcion(evento.getId())).thenReturn(false);

        assertThatThrownBy(() -> eventoService.recibir(evento.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("RECEPCION");
        assertThat(evento.getEstado()).isEqualTo(EstadoEvento.PLANEADO);
    }

    @Test
    void recibirConProductosYFotoPasaARecibido() {
        Evento evento = eventoEn(EstadoEvento.PLANEADO);
        conItem(evento, producto("Copa", TipoProducto.CRISTALERIA), 10);
        when(evidenciaFotografica.hayFotosDeRecepcion(evento.getId())).thenReturn(true);

        EventoResponse respuesta = eventoService.recibir(evento.getId());

        assertThat(respuesta.estado()).isEqualTo(EstadoEvento.RECIBIDO);
    }

    @Test
    void conLaEvidenciaDesactivadaNoSeConsultanLasFotos() {
        ReflectionTestUtils.setField(eventoService, "evidenciaObligatoria", false);
        Evento evento = eventoEn(EstadoEvento.PLANEADO);
        conItem(evento, producto("Copa", TipoProducto.CRISTALERIA), 10);

        eventoService.recibir(evento.getId());

        verifyNoInteractions(evidenciaFotografica);
        assertThat(evento.getEstado()).isEqualTo(EstadoEvento.RECIBIDO);
    }

    @Test
    void recibirUnEventoYaRecibidoFalla() {
        Evento evento = eventoEn(EstadoEvento.RECIBIDO);

        assertThatThrownBy(() -> eventoService.recibir(evento.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("RECIBIDO");
    }

    @Test
    void noSePuedenCambiarLosProductosDespuesDeRecibir() {
        Evento evento = eventoEn(EstadoEvento.RECIBIDO);
        UUID productoId = UUID.randomUUID();

        assertThatThrownBy(() -> eventoService.registrarItem(evento.getId(), productoId,
                new EventoItemRequest(10, null)))
                .isInstanceOf(BusinessRuleException.class);
    }

    // devolución

    @Test
    void laSumaDeLaDevolucionNoPuedeSuperarLoRecibido() {
        Evento evento = eventoEn(EstadoEvento.RECIBIDO);
        Producto copa = producto("Copa", TipoProducto.CRISTALERIA);
        conItem(evento, copa, 48);

        assertThatThrownBy(() -> eventoService.registrarDevolucion(evento.getId(), copa.getId(),
                new DevolucionItemRequest(46, 3, 0, 0)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("solo se recibieron 48");
    }

    @Test
    void soloLosLicoresPuedenConsumirse() {
        Evento evento = eventoEn(EstadoEvento.RECIBIDO);
        Producto copa = producto("Copa", TipoProducto.CRISTALERIA);
        conItem(evento, copa, 48);

        assertThatThrownBy(() -> eventoService.registrarDevolucion(evento.getId(), copa.getId(),
                new DevolucionItemRequest(40, 0, 0, 8)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("licores");
    }

    @Test
    void unLicorPuedeRegistrarConsumidas() {
        Evento evento = eventoEn(EstadoEvento.RECIBIDO);
        Producto ron = producto("Ron", TipoProducto.LICOR);
        conItem(evento, ron, 24);

        EventoItemResponse respuesta = eventoService.registrarDevolucion(evento.getId(), ron.getId(),
                new DevolucionItemRequest(6, 0, 0, 18));

        assertThat(respuesta.cantidadConsumida()).isEqualTo(18);
        assertThat(respuesta.diferencia()).isZero();
    }

    @Test
    void devolverConUnProductoSinCuadrarNombraElProducto() {
        Evento evento = eventoEn(EstadoEvento.RECIBIDO);
        EventoItem item = conItem(evento, producto("Copa", TipoProducto.CRISTALERIA), 48);
        item.registrarDevolucion(45, 0, 0, 0);

        assertThatThrownBy(() -> eventoService.devolver(evento.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("Copa")
                .hasMessageContaining("sin justificar: 3");
        assertThat(evento.getEstado()).isEqualTo(EstadoEvento.RECIBIDO);
    }

    @Test
    void devolverSinFotoDeDevolucionFalla() {
        Evento evento = eventoEn(EstadoEvento.RECIBIDO);
        EventoItem item = conItem(evento, producto("Copa", TipoProducto.CRISTALERIA), 10);
        item.registrarDevolucion(10, 0, 0, 0);
        when(evidenciaFotografica.hayFotosDeDevolucion(evento.getId())).thenReturn(false);

        assertThatThrownBy(() -> eventoService.devolver(evento.getId()))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("DEVOLUCION");
    }

    @Test
    void devolverCuandoCuadraYHayFotoPasaADevuelto() {
        Evento evento = eventoEn(EstadoEvento.RECIBIDO);
        EventoItem item = conItem(evento, producto("Copa", TipoProducto.CRISTALERIA), 48);
        item.registrarDevolucion(45, 2, 1, 0);
        when(evidenciaFotografica.hayFotosDeDevolucion(evento.getId())).thenReturn(true);

        assertThat(eventoService.devolver(evento.getId()).estado()).isEqualTo(EstadoEvento.DEVUELTO);
    }

    //cierre
    @Test
    void soloSePuedeCerrarUnEventoDevuelto() {
        Evento recibido = eventoEn(EstadoEvento.RECIBIDO);
        assertThatThrownBy(() -> eventoService.cerrar(recibido.getId()))
                .isInstanceOf(BusinessRuleException.class);

        Evento devuelto = eventoEn(EstadoEvento.DEVUELTO);
        assertThat(eventoService.cerrar(devuelto.getId()).estado()).isEqualTo(EstadoEvento.CERRADO);
    }

    // Metodos auxiliares

    private Evento eventoEn(EstadoEvento estado) {
        Evento evento = new Evento(new Planeador("Aurora"), "Boda", "Finca", LocalDate.of(2026, 12, 12));
        ReflectionTestUtils.setField(evento, "id", UUID.randomUUID());
        evento.setEstado(estado);
        when(eventoRepository.findConDetalleById(evento.getId())).thenReturn(Optional.of(evento));
        return evento;
    }

    private Producto producto(String nombre, TipoProducto tipo) {
        Producto producto = new Producto(nombre, tipo);
        ReflectionTestUtils.setField(producto, "id", UUID.randomUUID());
        return producto;
    }

    private EventoItem conItem(Evento evento, Producto producto, int recibida) {
        EventoItem item = evento.agregarItem(producto);
        item.setCantidadRecibida(recibida);
        return item;
    }
}
