package com.eventos.inventario.integracion;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Base64;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestcontainersConfig.class)
public abstract class IntegrationTestBase {

    /** PNG válido de 1x1 píxel. */
    protected static final byte[] PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==");

    @Autowired
    protected MockMvc mvc;

    protected String bearer;

    @BeforeEach
    void autenticar() throws Exception {
        String cuerpo = mvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"tester\",\"password\":\"test-password-123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(UTF_8);
        bearer = "Bearer " + JsonPath.<String>read(cuerpo, "$.accessToken");
    }

    // ---------- peticiones autenticadas ----------

    protected ResultActions enviarGet(String url) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.get(url).header(HttpHeaders.AUTHORIZATION, bearer));
    }

    protected ResultActions enviarPost(String url, String json) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post(url)
                .header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected ResultActions enviarPostVacio(String url) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post(url).header(HttpHeaders.AUTHORIZATION, bearer));
    }

    protected ResultActions enviarPut(String url, String json) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.put(url)
                .header(HttpHeaders.AUTHORIZATION, bearer)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected ResultActions enviarDelete(String url) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.delete(url).header(HttpHeaders.AUTHORIZATION, bearer));
    }

    protected ResultActions subirFoto(String eventoId, String momento, String productoId,
                                      byte[] contenido) throws Exception {
        var peticion = MockMvcRequestBuilders.multipart("/api/eventos/{id}/fotos", eventoId)
                .file(new MockMultipartFile("file", "foto.png", "image/png", contenido))
                .param("momento", momento)
                .header(HttpHeaders.AUTHORIZATION, bearer);
        if (productoId != null) {
            peticion.param("productoId", productoId);
        }
        return mvc.perform(peticion);
    }

    // ---------- creación de datos (cada prueba crea los suyos) ----------

    protected String crearPlaneador() throws Exception {
        return idDe(enviarPost("/api/planeadores",
                "{\"nombre\":\"Planeador " + UUID.randomUUID() + "\"}").andExpect(status().isCreated()));
    }

    protected String crearProducto(String nombre, String tipo) throws Exception {
        return idDe(enviarPost("/api/productos",
                "{\"nombre\":\"%s\",\"tipo\":\"%s\"}".formatted(nombre, tipo)).andExpect(status().isCreated()));
    }

    protected String crearEvento(String planeadorId) throws Exception {
        return idDe(enviarPost("/api/eventos",
                ("{\"planeadorId\":\"%s\",\"nombre\":\"Evento %s\",\"lugar\":\"Salon\","
                        + "\"fecha\":\"2026-12-12\"}").formatted(planeadorId, UUID.randomUUID()))
                .andExpect(status().isCreated()));
    }

    protected void registrarItem(String eventoId, String productoId, int cantidad) throws Exception {
        enviarPut("/api/eventos/%s/items/%s".formatted(eventoId, productoId),
                "{\"cantidadRecibida\":%d}".formatted(cantidad)).andExpect(status().isOk());
    }

    protected void subirFotoOk(String eventoId, String momento) throws Exception {
        subirFoto(eventoId, momento, null, PNG).andExpect(status().isCreated());
    }

    // ---------- lectura de respuestas ----------

    protected String idDe(ResultActions resultado) throws Exception {
        return JsonPath.<String>read(
                resultado.andReturn().getResponse().getContentAsString(UTF_8), "$.id");
    }

    protected String detalleDe(ResultActions resultado) throws Exception {
        return JsonPath.<String>read(
                resultado.andReturn().getResponse().getContentAsString(UTF_8), "$.detail");
    }
}