package com.emplanorte.service;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * RNF05 / CP-47 — Bloqueo temporal tras varios intentos fallidos de login.
 *
 * Lleva un conteo en memoria de intentos fallidos por correo. Cuando se alcanza
 * el máximo permitido, el correo queda bloqueado hasta que un login exitoso
 * (o el reinicio del conteo) lo libere.
 *
 * Nota: implementación en memoria, suficiente para un único administrador. En un
 * escenario multi-instancia debería respaldarse en una caché distribuida (Redis).
 */
@Service
public class LoginAttemptService {

    public static final int MAX_INTENTOS = 5;

    private final Map<String, Integer> intentosFallidos = new ConcurrentHashMap<>();

    public boolean estaBloqueado(String correo) {
        return intentosFallidos.getOrDefault(correo, 0) >= MAX_INTENTOS;
    }

    public void registrarFallo(String correo) {
        intentosFallidos.merge(correo, 1, Integer::sum);
    }

    public void registrarExito(String correo) {
        intentosFallidos.remove(correo);
    }

    /** Reinicia todo el conteo (útil para pruebas o tareas de mantenimiento). */
    public void reiniciar() {
        intentosFallidos.clear();
    }
}
