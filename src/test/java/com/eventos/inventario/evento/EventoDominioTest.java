package com.eventos.inventario.evento;

import com.eventos.inventario.planeador.Planeador;
import com.eventos.inventario.producto.Producto;
import com.eventos.inventario.producto.TipoProducto;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EventoDominioTest {

    private Evento evento() {
        return new Evento(new Planeador("Aurora"), "Boda", "Finca", LocalDate.of(2026, 12, 12));
    }

    private Producto producto(String nombre, TipoProducto tipo) {
        Producto p = new Producto(nombre, tipo);
        //ReflectionTestUtils asigna el id que normalmente pone la base de datos para poder probar sin persistir nada
        ReflectionTestUtils.setField(p, "id", UUID.randomUUID());
        return p;
    }

    @Test
    void laDiferenciaEsLoRecibidoMenosLoJustificado() {
        EventoItem item = evento().agregarItem(producto("Copa", TipoProducto.CRISTALERIA));
        item.setCantidadRecibida(48);
        item.registrarDevolucion(45, 2, 0, 0);

        assertThat(item.cantidadContabilizada()).isEqualTo(47);
        assertThat(item.diferencia()).isEqualTo(1);
    }

    @Test
    void registrarDevolucionReemplazaLasCuatroCantidades() {
        EventoItem item = evento().agregarItem(producto("Ron", TipoProducto.LICOR));
        item.setCantidadRecibida(12);
        item.registrarDevolucion(3, 0, 0, 9);
        item.registrarDevolucion(4, 0, 0, 8);

        assertThat(item.getCantidadDevuelta()).isEqualTo(4);
        assertThat(item.getCantidadConsumida()).isEqualTo(8);
        assertThat(item.diferencia()).isZero();
    }

    @Test
    void itemsSinCuadrarSoloIncluyeLosQueTienenDiferencia() {
        Evento evento = evento();
        EventoItem cuadra = evento.agregarItem(producto("Copa", TipoProducto.CRISTALERIA));
        cuadra.setCantidadRecibida(10);
        cuadra.registrarDevolucion(10, 0, 0, 0);
        EventoItem noCuadra = evento.agregarItem(producto("Plato", TipoProducto.MENAJE));
        noCuadra.setCantidadRecibida(10);
        noCuadra.registrarDevolucion(7, 0, 0, 0);

        assertThat(evento.itemsSinCuadrar()).containsExactly(noCuadra);
    }

    @Test
    void buscarItemEncuentraPorProducto() {
        Evento evento = evento();
        Producto copa = producto("Copa", TipoProducto.CRISTALERIA);
        evento.agregarItem(copa);

        assertThat(evento.buscarItem(copa.getId())).isPresent();
        assertThat(evento.buscarItem(UUID.randomUUID())).isEmpty();
    }
}