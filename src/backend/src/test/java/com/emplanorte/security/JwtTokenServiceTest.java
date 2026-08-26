package com.emplanorte.security;

import com.emplanorte.model.Usuario;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenServiceTest {

    private static final String SECRET = "EMPLANORTE_TEST_SECRET_WITH_MORE_THAN_32_BYTES_2026";
    private static final Instant NOW = Instant.parse("2026-08-25T12:00:00Z");

    @Test
    void tokenValidoDura24HorasYConservaLaIdentidad() {
        JwtTokenService service = serviceAt(NOW, SECRET, 24);

        JwtTokenService.IssuedToken issued = service.generateToken(usuario());
        JwtTokenService.TokenClaims claims = service.validateToken(issued.value());

        assertThat(issued.expiresInSeconds()).isEqualTo(86_400);
        assertThat(issued.expiresAt()).isEqualTo(NOW.plusSeconds(86_400));
        assertThat(claims.userId()).isEqualTo(7L);
        assertThat(claims.correo()).isEqualTo("usuario.real@emplanorte.com");
        assertThat(claims.rol()).isEqualTo("administrador");
    }

    @Test
    void duracionEsConfigurable() {
        JwtTokenService service = serviceAt(NOW, SECRET, 6);

        JwtTokenService.IssuedToken issued = service.generateToken(usuario());

        assertThat(issued.expiresInSeconds()).isEqualTo(21_600);
        assertThat(issued.expiresAt()).isEqualTo(NOW.plusSeconds(21_600));
    }

    @Test
    void tokenExpiradoDevuelveMensajeClaro() {
        String token = serviceAt(NOW, SECRET, 24).generateToken(usuario()).value();
        JwtTokenService validator = serviceAt(NOW.plusSeconds(86_401), SECRET, 24);

        assertThatThrownBy(() -> validator.validateToken(token))
                .isInstanceOf(JwtTokenService.ExpiredTokenException.class)
                .hasMessage("Tu sesión ha expirado. Inicia sesión nuevamente.");
    }

    @Test
    void tokenFirmadoConOtraClaveEsInvalido() {
        String token = serviceAt(NOW, SECRET, 24).generateToken(usuario()).value();
        JwtTokenService validator = serviceAt(
                NOW,
                "OTRA_CLAVE_TOTALMENTE_DISTINTA_Y_MAYOR_DE_32_BYTES",
                24
        );

        assertThatThrownBy(() -> validator.validateToken(token))
                .isInstanceOf(JwtTokenService.InvalidTokenException.class)
                .hasMessage("Token inválido. Inicia sesión nuevamente.");
    }

    @Test
    void tokenMalformadoEsInvalido() {
        JwtTokenService service = serviceAt(NOW, SECRET, 24);

        assertThatThrownBy(() -> service.validateToken("esto-no-es-un-token"))
                .isInstanceOf(JwtTokenService.InvalidTokenException.class);
    }

    @Test
    void rechazaSecretosConfiguradosDemasiadoCortos() {
        assertThatThrownBy(() -> serviceAt(NOW, "clave-corta", 24))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32 bytes");
    }

    private JwtTokenService serviceAt(Instant instant, String secret, long hours) {
        return new JwtTokenService(
                secret,
                hours,
                new ObjectMapper(),
                Clock.fixed(instant, ZoneOffset.UTC)
        );
    }

    private Usuario usuario() {
        Usuario usuario = new Usuario();
        usuario.setId(7L);
        usuario.setNombre("Usuario Real");
        usuario.setCorreo("usuario.real@emplanorte.com");
        usuario.setRol("administrador");
        usuario.setActivo(true);
        return usuario;
    }
}
