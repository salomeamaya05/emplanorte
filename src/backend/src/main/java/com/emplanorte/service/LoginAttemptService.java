package com.emplanorte.service;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RNF05 / CP-47 — Bloqueo temporal tras varios intentos fallidos de login.
 *
 * Lleva un conteo en memoria de intentos fallidos por correo. Cuando se alcanza
 * el máximo permitido, el correo queda bloqueado durante un intervalo corto.
 *
 * Nota: implementación en memoria, suficiente para un único administrador. En un
 * escenario multi-instancia debería respaldarse en una caché distribuida (Redis).
 */
@Service
public class LoginAttemptService {

    public static final int MAX_INTENTOS = 10;
    public static final Duration DURACION_BLOQUEO = Duration.ofMinutes(15);

    private final Map<String, EstadoIntentos> intentosPorCorreo = new ConcurrentHashMap<>();
    private final Clock clock;

    public LoginAttemptService() {
        this(Clock.systemUTC());
    }

    LoginAttemptService(Clock clock) {
        this.clock = clock;
    }

    public boolean estaBloqueado(String correo) {
        String clave = normalizar(correo);
        EstadoIntentos estado = intentosPorCorreo.get(clave);
        if (estado == null || estado.bloqueadoHasta() == null) {
            return false;
        }
        if (!Instant.now(clock).isBefore(estado.bloqueadoHasta())) {
            intentosPorCorreo.remove(clave, estado);
            return false;
        }
        return true;
    }

    /** Registra el fallo y devuelve cuántos intentos quedan antes del bloqueo. */
    public int registrarFallo(String correo) {
        String clave = normalizar(correo);
        EstadoIntentos estado = intentosPorCorreo.compute(clave, (ignorado, actual) -> {
            int fallos = actual == null ? 1 : Math.min(actual.fallos() + 1, MAX_INTENTOS);
            Instant bloqueadoHasta = fallos >= MAX_INTENTOS
                    ? Instant.now(clock).plus(DURACION_BLOQUEO)
                    : null;
            return new EstadoIntentos(fallos, bloqueadoHasta);
        });
        return Math.max(0, MAX_INTENTOS - estado.fallos());
    }

    public void registrarExito(String correo) {
        intentosPorCorreo.remove(normalizar(correo));
    }

    /** Reinicia todo el conteo (útil para pruebas o tareas de mantenimiento). */
    public void reiniciar() {
        intentosPorCorreo.clear();
    }

    private String normalizar(String correo) {
        return correo == null ? "" : correo.trim().toLowerCase(Locale.ROOT);
    }

    private record EstadoIntentos(int fallos, Instant bloqueadoHasta) {}
}
