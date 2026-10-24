package com.eventos.inventario.integracion;

import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class EventoFlujoIntegrationTest extends IntegrationTestBase {

    @Test
    void cicloCompletoRecibeDevuelveCierraYReporta() throws Exception {
        String evento = crearEvento(crearPlaneador());
        String copa = crearProducto("Copa de vino", "CRISTALERIA");
        String ron = crearProducto("Ron 750 ml", "LICOR");

        registrarItem(evento, copa, 30);
        registrarItem(evento, ron, 12);
        subirFotoOk(evento, "RECEPCION");
        enviarPostVacio(ruta(evento, "/recibir"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("RECIBIDO"));

        registrarDevolucion(evento, copa, 29, 1, 0, 0).andExpect(status().isOk());
        registrarDevolucion(evento, ron, 3, 0, 0, 9).andExpect(status().isOk());
        subirFotoOk(evento, "DEVOLUCION");
        enviarPostVacio(ruta(evento, "/devolver"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("DEVUELTO"));
        enviarPostVacio(ruta(evento, "/cerrar"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("CERRADO"));

        enviarGet(ruta(evento, "/reporte"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cuadra").value(true))
                .andExpect(jsonPath("$.fotos.length()").value(2))
                .andExpect(jsonPath("$.totalesPorTipo[?(@.tipo == 'CRISTALERIA')].rota").value(1))
                .andExpect(jsonPath("$.totalesPorTipo[?(@.tipo == 'LICOR')].consumida").value(9));
    }

    @Test
    void registrarElMismoProductoDosVecesNoDuplicaElItem() throws Exception {
        String evento = crearEvento(crearPlaneador());
        String copa = crearProducto("Copa", "CRISTALERIA");

        registrarItem(evento, copa, 50);
        registrarItem(evento, copa, 48);

        enviarGet("/api/eventos/" + evento)
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].cantidadRecibida").value(48));
    }

    @Test
    void noSePuedeRecibirSinProductos() throws Exception {
        String evento = crearEvento(crearPlaneador());

        ResultActions respuesta = enviarPostVacio(ruta(evento, "/recibir")).andExpect(status().isConflict());
        assertThat(detalleDe(respuesta)).contains("sin productos");
    }

    @Test
    void noSePuedeRecibirSinFotoDeRecepcion() throws Exception {
        String evento = crearEvento(crearPlaneador());
        registrarItem(evento, crearProducto("Copa", "CRISTALERIA"), 10);

        ResultActions respuesta = enviarPostVacio(ruta(evento, "/recibir")).andExpect(status().isConflict());
        assertThat(detalleDe(respuesta)).contains("RECEPCION");
    }

    @Test
    void despuesDeRecibirNoSePuedenCambiarLosProductos() throws Exception {
        String copa = crearProducto("Copa", "CRISTALERIA");
        String evento = eventoRecibidoCon(copa, 10);

        enviarPut("/api/eventos/%s/items/%s".formatted(evento, copa), "{\"cantidadRecibida\":99}")
                .andExpect(status().isConflict());
        enviarDelete("/api/eventos/%s/items/%s".formatted(evento, copa))
                .andExpect(status().isConflict());
    }

    @Test
    void noSePuedeDevolverSiAlgunProductoNoCuadra() throws Exception {
        String copa = crearProducto("Copa descuadrada", "CRISTALERIA");
        String evento = eventoRecibidoCon(copa, 48);
        registrarDevolucion(evento, copa, 45, 0, 0, 0).andExpect(status().isOk());
        subirFotoOk(evento, "DEVOLUCION");

        ResultActions respuesta = enviarPostVacio(ruta(evento, "/devolver")).andExpect(status().isConflict());
        assertThat(detalleDe(respuesta)).contains("Copa descuadrada").contains("sin justificar: 3");
    }

    @Test
    void laSumaDeLaDevolucionNoPuedeSuperarLoRecibido() throws Exception {
        String copa = crearProducto("Copa", "CRISTALERIA");
        String evento = eventoRecibidoCon(copa, 48);

        registrarDevolucion(evento, copa, 46, 3, 0, 0).andExpect(status().isConflict());
    }

    @Test
    void soloLosLicoresPuedenConsumirse() throws Exception {
        String copa = crearProducto("Copa", "CRISTALERIA");
        String evento = eventoRecibidoCon(copa, 48);

        registrarDevolucion(evento, copa, 40, 0, 0, 8).andExpect(status().isConflict());
    }

    @Test
    void faltandoUnCampoDeLaDevolucionDa400() throws Exception {
        String copa = crearProducto("Copa", "CRISTALERIA");
        String evento = eventoRecibidoCon(copa, 48);

        enviarPut("/api/eventos/%s/items/%s/devolucion".formatted(evento, copa),
                "{\"cantidadDevuelta\":45,\"cantidadRota\":2}").andExpect(status().isBadRequest());
    }

    @Test
    void noSePuedeDevolverSinFotoDeDevolucion() throws Exception {
        String copa = crearProducto("Copa", "CRISTALERIA");
        String evento = eventoRecibidoCon(copa, 10);
        registrarDevolucion(evento, copa, 10, 0, 0, 0).andExpect(status().isOk());

        ResultActions respuesta = enviarPostVacio(ruta(evento, "/devolver")).andExpect(status().isConflict());
        assertThat(detalleDe(respuesta)).contains("DEVOLUCION");
    }

    @Test
    void unEventoCerradoNoAceptaMasFotos() throws Exception {
        String copa = crearProducto("Copa", "CRISTALERIA");
        String evento = eventoRecibidoCon(copa, 10);
        registrarDevolucion(evento, copa, 10, 0, 0, 0).andExpect(status().isOk());
        subirFotoOk(evento, "DEVOLUCION");
        enviarPostVacio(ruta(evento, "/devolver")).andExpect(status().isOk());
        enviarPostVacio(ruta(evento, "/cerrar")).andExpect(status().isOk());

        subirFoto(evento, "DEVOLUCION", null, PNG).andExpect(status().isConflict());
    }

    @Test
    void unPlaneadorConEventosNoSePuedeEliminar() throws Exception {
        String planeador = crearPlaneador();
        crearEvento(planeador);

        enviarDelete("/api/planeadores/" + planeador).andExpect(status().isConflict());
        enviarGet("/api/planeadores/" + planeador).andExpect(status().isOk());
    }

    @Test
    void unEventoInexistenteDa404() throws Exception {
        enviarGet("/api/eventos/" + UUID.randomUUID()).andExpect(status().isNotFound());
    }

    // ---------- ayudas ----------

    private String ruta(String evento, String sufijo) {
        return "/api/eventos/" + evento + sufijo;
    }

    private String eventoRecibidoCon(String productoId, int cantidad) throws Exception {
        String evento = crearEvento(crearPlaneador());
        registrarItem(evento, productoId, cantidad);
        subirFotoOk(evento, "RECEPCION");
        enviarPostVacio(ruta(evento, "/recibir")).andExpect(status().isOk());
        return evento;
    }

    private ResultActions registrarDevolucion(String evento, String producto, int devuelta, int rota,
                                              int perdida, int consumida) throws Exception {
        return enviarPut("/api/eventos/%s/items/%s/devolucion".formatted(evento, producto),
                ("{\"cantidadDevuelta\":%d,\"cantidadRota\":%d,\"cantidadPerdida\":%d,"
                        + "\"cantidadConsumida\":%d}").formatted(devuelta, rota, perdida, consumida));
    }
}