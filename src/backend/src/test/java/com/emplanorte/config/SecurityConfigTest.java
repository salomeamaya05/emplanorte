package com.emplanorte.config;

import com.emplanorte.controller.ProductoController;
import com.emplanorte.security.JwtTokenService;
import com.emplanorte.service.ProductoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ProductoController.class)
@Import(SecurityConfig.class)
class SecurityConfigTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private ProductoService productoService;
    @MockBean private JwtTokenService jwtTokenService;

    @BeforeEach
    void configureService() {
        when(productoService.obtenerTodosActivos()).thenReturn(List.of());
        when(jwtTokenService.validateToken("token-valido")).thenReturn(
                new JwtTokenService.TokenClaims(
                        1L,
                        "admin@emplanorte.com",
                        "Administrador",
                        "administrador",
                        Instant.parse("2026-08-25T00:00:00Z"),
                        Instant.parse("2026-08-26T00:00:00Z")
                )
        );
        when(jwtTokenService.validateToken("token-expirado"))
                .thenThrow(new JwtTokenService.ExpiredTokenException());
        when(jwtTokenService.validateToken("token-invalido"))
                .thenThrow(new JwtTokenService.InvalidTokenException());
    }

    @Test
    void accesoSinTokenEsRechazado() throws Exception {
        mockMvc.perform(get("/api/productos"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTENTICACION_REQUERIDA"))
                .andExpect(jsonPath("$.message").value("Debes iniciar sesión para continuar."));
    }

    @Test
    void tokenValidoPermiteAcceso() throws Exception {
        mockMvc.perform(get("/api/productos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token-valido"))
                .andExpect(status().isOk());
    }

    @Test
    void tokenExpiradoEsRechazadoConMensajeClaro() throws Exception {
        mockMvc.perform(get("/api/productos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token-expirado"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_EXPIRADO"))
                .andExpect(jsonPath("$.message")
                        .value("Tu sesión ha expirado. Inicia sesión nuevamente."));
    }

    @Test
    void tokenInvalidoEsRechazado() throws Exception {
        mockMvc.perform(get("/api/productos")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer token-invalido"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("TOKEN_INVALIDO"));
    }
}
