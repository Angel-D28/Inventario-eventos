package com.eventos.inventario.integracion;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIntegrationTest extends IntegrationTestBase {

    @Autowired
    JwtEncoder jwtEncoder;

    @Test
    void loginCorrectoDevuelveUnTokenBearer() throws Exception {
        login("tester", "test-password-123")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600));
    }

    @Test
    void contrasenaIncorrectaYUsuarioInexistenteDanElMismoMensaje() throws Exception {
        ResultActions contrasenaMala = login("tester", "incorrecta-12345").andExpect(status().isUnauthorized());
        ResultActions usuarioMalo = login("nadie", "incorrecta-12345").andExpect(status().isUnauthorized());

        assertThat(detalleDe(contrasenaMala)).isEqualTo(detalleDe(usuarioMalo));
    }

    @Test
    void elLoginSinCamposDa400() throws Exception {
        mvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"username\":\"tester\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void sinTokenNoSePuedeAccederALaApi() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/productos")).andExpect(status().isUnauthorized());
    }

    @Test
    void conTokenValidoSePuedeAccederALaApi() throws Exception {
        enviarGet("/api/productos").andExpect(status().isOk());
    }

    @Test
    void unTokenAlteradoEsRechazado() throws Exception {
        String[] partes = bearer.substring("Bearer ".length()).split("\\.");
        char original = partes[2].charAt(5);
        String firma = partes[2].substring(0, 5) + (original == 'A' ? 'B' : 'A') + partes[2].substring(6);

        mvc.perform(MockMvcRequestBuilders.get("/api/productos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + partes[0] + "." + partes[1] + "." + firma))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unTokenExpiradoEsRechazado() throws Exception {
        Instant ahora = Instant.now();
        String token = tokenCon("inventario-api", ahora.minus(2, ChronoUnit.HOURS), ahora.minus(1, ChronoUnit.HOURS));

        mvc.perform(MockMvcRequestBuilders.get("/api/productos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void unTokenDeOtroEmisorEsRechazado() throws Exception {
        Instant ahora = Instant.now();
        String token = tokenCon("otro-sistema", ahora, ahora.plus(1, ChronoUnit.HOURS));

        mvc.perform(MockMvcRequestBuilders.get("/api/productos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions login(String usuario, String contrasena) throws Exception {
        return mvc.perform(MockMvcRequestBuilders.post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(usuario, contrasena)));
    }

    /** Firma un token con la clave real, pero con el emisor y las fechas que queramos. */
    private String tokenCon(String emisor, Instant emision, Instant expiracion) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(emisor).subject("tester").issuedAt(emision).expiresAt(expiracion).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(
                JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}