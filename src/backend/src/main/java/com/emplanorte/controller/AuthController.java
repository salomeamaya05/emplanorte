package com.emplanorte.controller;

import com.emplanorte.model.Usuario;
import com.emplanorte.service.AuthService;
import com.emplanorte.service.LoginAttemptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    // Formato de correo: parte local, @, dominio y al menos un punto. Sin espacios.
    private static final Pattern CORREO_VALIDO =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    @Autowired
    private AuthService authService;

    @Autowired
    private LoginAttemptService loginAttemptService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String correo = credentials.get("correo");
        String contrasena = credentials.get("contrasena");

        // Validación de presencia (no nulos ni en blanco)
        if (esVacio(correo) || esVacio(contrasena)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("El correo y la contraseña son obligatorios");
        }

        // CP-47 - Bloqueo temporal tras demasiados intentos fallidos
        if (loginAttemptService.estaBloqueado(correo)) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body("Demasiados intentos fallidos. Intente de nuevo más tarde.");
        }

        Optional<Usuario> usuarioOpt = authService.login(correo, contrasena);

        if (usuarioOpt.isPresent()) {
            loginAttemptService.registrarExito(correo);
            Usuario usuario = usuarioOpt.get();
            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Inicio de sesión exitoso");
            response.put("id", usuario.getId());
            response.put("nombre", usuario.getNombre());
            response.put("correo", usuario.getCorreo());
            response.put("rol", usuario.getRol());
            // En el futuro se puede generar un token JWT aquí
            response.put("token", "dummy-jwt-token-for-" + usuario.getId());
            return ResponseEntity.ok(response);
        } else {
            loginAttemptService.registrarFallo(correo);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Credenciales incorrectas o usuario inactivo");
        }
    }

    /**
     * Registro de usuario. Aquí SÍ corresponde validar el formato del correo y
     * las reglas de complejidad de la contraseña, porque es el momento en que el
     * usuario las elige (a diferencia del login, que solo verifica credenciales).
     */
    @PostMapping("/registro")
    public ResponseEntity<?> registro(@RequestBody Map<String, String> body) {
        String correo = body.get("correo");
        String contrasena = body.get("contrasena");
        String nombre = body.get("nombre");
        String rol = body.get("rol");

        if (esVacio(correo) || esVacio(contrasena) || esVacio(nombre)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("El nombre, el correo y la contraseña son obligatorios");
        }
        if (!CORREO_VALIDO.matcher(correo.trim()).matches()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("El correo no tiene un formato válido");
        }
        if (contrasena.length() < 8) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("La contraseña debe tener al menos 8 caracteres");
        }
        if (contrasena.chars().noneMatch(Character::isUpperCase)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("La contraseña debe tener al menos una letra mayúscula");
        }

        try {
            Usuario usuario = authService.registrar(correo.trim(), contrasena, nombre.trim(), rol);
            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Usuario registrado correctamente");
            response.put("id", usuario.getId());
            response.put("nombre", usuario.getNombre());
            response.put("correo", usuario.getCorreo());
            response.put("rol", usuario.getRol());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            // Correo ya registrado u otra violación de regla de negocio
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    private boolean esVacio(String valor) {
        return valor == null || valor.isBlank();
    }
}
