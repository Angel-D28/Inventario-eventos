package com.eventos.inventario.integracion;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FotoIntegrationTest extends IntegrationTestBase {

    @Test
    void subirYDescargarDevuelveLosMismosBytes() throws Exception {
        String evento = crearEvento(crearPlaneador());
        String copa = crearProducto("Copa", "CRISTALERIA");
        registrarItem(evento, copa, 10);

        String respuesta = subirFoto(evento, "RECEPCION", copa, PNG)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.momento").value("RECEPCION"))
                .andExpect(jsonPath("$.productoId").value(copa))
                .andReturn().getResponse().getContentAsString(UTF_8);
        String url = JsonPath.read(respuesta, "$.url");

        byte[] descargada = enviarGet(url)
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(descargada).isEqualTo(PNG);
    }

    @Test
    void unArchivoQueNoEsImagenSeRechazaAunqueDigaSerPng() throws Exception {
        String evento = crearEvento(crearPlaneador());

        subirFoto(evento, "RECEPCION", null, "esto no es una imagen".getBytes(UTF_8))
                .andExpect(status().isBadRequest());
    }

    @Test
    void unProductoAjenoAlEventoSeRechaza() throws Exception {
        String evento = crearEvento(crearPlaneador());
        String ajeno = crearProducto("Producto ajeno", "MENAJE");

        subirFoto(evento, "RECEPCION", ajeno, PNG).andExpect(status().isConflict());
    }

    @Test
    void laFotoDeDevolucionNoSeAceptaConElEventoEnPlaneado() throws Exception {
        String evento = crearEvento(crearPlaneador());

        subirFoto(evento, "DEVOLUCION", null, PNG).andExpect(status().isConflict());
    }

    @Test
    void unMomentoInvalidoDa400() throws Exception {
        String evento = crearEvento(crearPlaneador());

        subirFoto(evento, "ENTREGA", null, PNG).andExpect(status().isBadRequest());
    }

    @Test
    void sinTokenNoSeDescargaNingunaFoto() throws Exception {
        String evento = crearEvento(crearPlaneador());
        String respuesta = subirFoto(evento, "RECEPCION", null, PNG)
                .andReturn().getResponse().getContentAsString(UTF_8);
        String url = JsonPath.read(respuesta, "$.url");

        mvc.perform(MockMvcRequestBuilders.get(url)).andExpect(status().isUnauthorized());
    }

    @Test
    void eliminarUnaFotoLaQuitaDelListado() throws Exception {
        String evento = crearEvento(crearPlaneador());
        String id = idDe(subirFoto(evento, "RECEPCION", null, PNG).andExpect(status().isCreated()));

        enviarDelete("/api/fotos/" + id).andExpect(status().isNoContent());

        enviarGet("/api/eventos/" + evento + "/fotos").andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void laFotoDeReferenciaDelProductoSeSubeYSeDescarga() throws Exception {
        String producto = crearProducto("Copa con foto", "CRISTALERIA");

        mvc.perform(MockMvcRequestBuilders.multipart(HttpMethod.PUT, "/api/productos/{id}/foto", producto)
                        .file(new MockMultipartFile("file", "ref.png", "image/png", PNG))
                        .header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fotoUrl").value("/api/productos/" + producto + "/foto"));

        byte[] descargada = enviarGet("/api/productos/" + producto + "/foto")
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(descargada).isEqualTo(PNG);
    }
}