package com.emplanorte.service;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;

class LoginAttemptServiceTest {

    @Test
    void bloqueaAlDecimoFalloYSeDesbloqueaDespuesDeQuinceMinutos() {
        RelojMutable reloj = new RelojMutable(Instant.parse("2026-08-18T12:00:00Z"));
        LoginAttemptService service = new LoginAttemptService(reloj);

        for (int intento = 1; intento <= 9; intento++) {
            assertThat(service.registrarFallo(" Admin@Emplanorte.com "))
                    .isEqualTo(LoginAttemptService.MAX_INTENTOS - intento);
            assertThat(service.estaBloqueado("admin@emplanorte.com")).isFalse();
        }

        assertThat(service.registrarFallo("admin@emplanorte.com")).isZero();
        assertThat(service.estaBloqueado("ADMIN@EMPLANORTE.COM")).isTrue();

        reloj.avanzarSegundos(LoginAttemptService.DURACION_BLOQUEO.toSeconds());

        assertThat(service.estaBloqueado("admin@emplanorte.com")).isFalse();
        assertThat(service.registrarFallo("admin@emplanorte.com")).isEqualTo(9);
    }

    @Test
    void loginExitosoReiniciaElContador() {
        LoginAttemptService service = new LoginAttemptService();
        service.registrarFallo("admin@emplanorte.com");
        service.registrarFallo("admin@emplanorte.com");

        service.registrarExito("admin@emplanorte.com");

        assertThat(service.registrarFallo("admin@emplanorte.com")).isEqualTo(9);
    }

    private static final class RelojMutable extends Clock {
        private Instant instante;

        private RelojMutable(Instant instante) {
            this.instante = instante;
        }

        void avanzarSegundos(long segundos) {
            instante = instante.plusSeconds(segundos);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return instante;
        }
    }
}
