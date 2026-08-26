package com.emplanorte.controller;

import com.emplanorte.model.Usuario;
import com.emplanorte.security.JwtTokenService;
import com.emplanorte.service.AuthService;
import com.emplanorte.service.LoginAttemptService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.emplanorte.config.SecurityConfig;

/**
 * Pruebas de integración HTTP del endpoint POST /api/auth/login (RNF05 — Control de acceso).
 *
 * Organización:
 *   - Bloques "ComportamientoActual" : verifican lo que el código HACE hoy (deben PASAR).
 *   - Bloque  "ValidacionesEsperadas" : codifican lo que el Plan/usuario ESPERA pero que
 *                                        el código todavía NO implementa. Estas pruebas
 *                                        FALLAN intencionalmente y quedan como evidencia
 *                                        documentada del gap (mismo criterio que CP-44).
 *
 * Cada @DisplayName que empieza con [GAP] señala una prueba que se espera en ROJO.
 */
@WebMvcTest(AuthController.class)
@Import({SecurityConfig.class, LoginAttemptService.class})
@DisplayName("AuthController — Integración HTTP /api/auth (RNF05)")
class AuthControllerTest {

    private static final String MSG_OBLIGATORIOS = "El correo y la contraseña son obligatorios";
    private static final String MSG_CREDENCIALES = "Correo o contraseña incorrectos";
    private static final String CORREO_OK = "duvan@emplanorte.com";
    private static final String CLAVE_OK = "Admin2024*";

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private LoginAttemptService loginAttemptService;
    @MockBean  private AuthService authService;
    @MockBean  private JwtTokenService jwtTokenService;

    private Usuario usuarioMock;

    @BeforeEach
    void setUp() {
        // El bloqueo por intentos fallidos es estado en memoria compartido entre
        // métodos de la misma clase de test: se reinicia para aislar cada prueba.
        loginAttemptService.reiniciar();

        usuarioMock = new Usuario();
        usuarioMock.setId(1L);
        usuarioMock.setNombre("Duvan Alvarado");
        usuarioMock.setCorreo(CORREO_OK);
        usuarioMock.setRol("administrador");
        usuarioMock.setActivo(true);

        when(jwtTokenService.generateToken(any(Usuario.class))).thenReturn(
                new JwtTokenService.IssuedToken(
                        "token-jwt-valido",
                        Instant.parse("2026-08-26T12:00:00Z"),
                        86_400
                )
        );
        when(jwtTokenService.validateToken("token-admin")).thenReturn(
                new JwtTokenService.TokenClaims(
                        1L,
                        CORREO_OK,
                        "Duvan Alvarado",
                        "administrador",
                        Instant.parse("2026-08-25T12:00:00Z"),
                        Instant.parse("2026-08-26T12:00:00Z")
                )
        );
    }

    private String json(Object body) throws Exception {
        return objectMapper.writeValueAsString(body);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CP-45 — Login exitoso (detallado): se valida CADA campo de la respuesta
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("CP-45 — Login exitoso (comportamiento actual, debe PASAR)")
    class LoginExitoso {

        @Test
        @DisplayName("CP-45a: credenciales válidas → 200 y la respuesta contiene TODOS los campos esperados")
        void login_valido_retornaTodosLosCampos() throws Exception {
            when(authService.login(CORREO_OK, CLAVE_OK)).thenReturn(Optional.of(usuarioMock));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("correo", CORREO_OK, "contrasena", CLAVE_OK))))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.mensaje").value("Inicio de sesión exitoso"))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.nombre").value("Duvan Alvarado"))
                    .andExpect(jsonPath("$.correo").value(CORREO_OK))
                    .andExpect(jsonPath("$.rol").value("administrador"))
                    .andExpect(jsonPath("$.token").value("token-jwt-valido"))
                    .andExpect(jsonPath("$.expiraEn").value("2026-08-26T12:00:00Z"))
                    .andExpect(jsonPath("$.duracionTokenSegundos").value(86_400));
        }

        @Test
        @DisplayName("CP-45b: la respuesta NUNCA expone el hash de la contraseña")
        void login_valido_noExponeHash() throws Exception {
            usuarioMock.setContrasenaHash("$2a$10$hashsecretobcrypt");
            when(authService.login(CORREO_OK, CLAVE_OK)).thenReturn(Optional.of(usuarioMock));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("correo", CORREO_OK, "contrasena", CLAVE_OK))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.contrasenaHash").doesNotExist())
                    .andExpect(content().string(Matchers.not(Matchers.containsString("$2a$10$"))));
        }

        @Test
        @DisplayName("CP-45c: el controlador delega en AuthService.login con EXACTAMENTE las credenciales recibidas")
        void login_valido_delegaConCredencialesExactas() throws Exception {
            when(authService.login(CORREO_OK, CLAVE_OK)).thenReturn(Optional.of(usuarioMock));

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("correo", CORREO_OK, "contrasena", CLAVE_OK))))
                    .andExpect(status().isOk());

            verify(authService, times(1)).login(CORREO_OK, CLAVE_OK);
            verifyNoMoreInteractions(authService);
        }

        @Test
        @DisplayName("CP-45d: campos extra en el JSON se ignoran y el login válido sigue dando 200")
        void login_conCamposExtra_seIgnoran() throws Exception {
            when(authService.login(CORREO_OK, CLAVE_OK)).thenReturn(Optional.of(usuarioMock));
            String body = "{\"correo\":\"" + CORREO_OK + "\",\"contrasena\":\"" + CLAVE_OK
                    + "\",\"campoExtra\":\"hola\",\"rol\":\"superadmin\"}";

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    // el rol viene del usuario real, NO del campo inyectado en el request
                    .andExpect(jsonPath("$.rol").value("administrador"));
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CP-46 — Credenciales inválidas (mensaje genérico, sin fuga de info)
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("CP-46 — Credenciales inválidas (comportamiento actual, debe PASAR)")
    class CredencialesInvalidas {

        @Test
        @DisplayName("CP-46a: contraseña incorrecta → 401 con mensaje EXACTO genérico")
        void login_contrasenaIncorrecta_retorna401MensajeExacto() throws Exception {
            when(authService.login(eq(CORREO_OK), any())).thenReturn(Optional.empty());

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("correo", CORREO_OK, "contrasena", "claveErrada"))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().string(Matchers.containsString(MSG_CREDENCIALES)));
        }

        @Test
        @DisplayName("CP-46b: usuario inexistente → 401 con el MISMO mensaje (no revela si el usuario existe)")
        void login_usuarioInexistente_mismoMensajeQueClaveMala() throws Exception {
            when(authService.login(any(), any())).thenReturn(Optional.empty());

            String respUsuarioMalo = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("correo", "noexiste@test.com", "contrasena", "x"))))
                    .andExpect(status().isUnauthorized())
                    .andReturn().getResponse().getContentAsString();

            String respClaveMala = mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("correo", CORREO_OK, "contrasena", "claveErrada"))))
                    .andExpect(status().isUnauthorized())
                    .andReturn().getResponse().getContentAsString();

            // Ambos errores son indistinguibles → no se filtra si el correo existe
            org.assertj.core.api.Assertions.assertThat(respUsuarioMalo).isEqualTo(respClaveMala);
        }

        @Test
        @DisplayName("CP-46c: usuario inactivo (service devuelve vacío) → 401, no 200")
        void login_usuarioInactivo_retorna401() throws Exception {
            when(authService.login(any(), any())).thenReturn(Optional.empty());

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("correo", CORREO_OK, "contrasena", CLAVE_OK))))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CP-46 (cont.) — Campos faltantes (null): el código SÍ los valida → 400
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("CP-46 — Campos null/ausentes (comportamiento actual, debe PASAR)")
    class CamposFaltantes {

        @Test
        @DisplayName("CP-46d: sin correo → 400 con mensaje EXACTO de campos obligatorios")
        void login_sinCorreo_retorna400ConMensaje() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("contrasena", CLAVE_OK))))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(Matchers.containsString(MSG_OBLIGATORIOS)));
            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("CP-46e: sin contraseña → 400 con mensaje EXACTO de campos obligatorios")
        void login_sinContrasena_retorna400ConMensaje() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("correo", CORREO_OK))))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(Matchers.containsString(MSG_OBLIGATORIOS)));
            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("CP-46f: body vacío {} → 400")
        void login_bodyVacio_retorna400() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(Matchers.containsString(MSG_OBLIGATORIOS)));
        }

        @Test
        @DisplayName("CP-46g: correo y contraseña explícitamente null en el JSON → 400")
        void login_camposNullExplicitos_retorna400() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"correo\":null,\"contrasena\":null}"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(authService);
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  CP-48 — Seguridad: inyección SQL en la capa HTTP (comportamiento actual)
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("CP-48 — Inyección SQL (comportamiento actual, debe PASAR)")
    class InyeccionSql {

        @Test
        @DisplayName("CP-48a: usuario \"admin' OR '1'='1\" → se trata como credencial normal, 401 sin error 500")
        void login_inyeccionSqlEnCorreo_retorna401SinEjecutar() throws Exception {
            when(authService.login(any(), any())).thenReturn(Optional.empty());

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("correo", "admin' OR '1'='1", "contrasena", "x"))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(content().string(Matchers.not(Matchers.containsString("SQL"))));
        }

        @Test
        @DisplayName("CP-48b: payload con '; DROP TABLE en la contraseña → 401, nunca 200")
        void login_inyeccionSqlEnContrasena_noAutentica() throws Exception {
            when(authService.login(any(), any())).thenReturn(Optional.empty());

            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("correo", CORREO_OK, "contrasena", "'; DROP TABLE usuarios;--"))))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  Robustez del endpoint (comportamiento actual)
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Robustez HTTP (comportamiento actual, debe PASAR)")
    class RobustezHttp {

        @Test
        @DisplayName("ROB-1: JSON malformado → 400")
        void login_jsonMalformado_retorna400() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"correo\": \"x\", "))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("ROB-2: petición sin body → 400")
        void login_sinBody_retorna400() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("ROB-3: Content-Type text/plain en vez de JSON → 415 Unsupported Media Type")
        void login_contentTypeInvalido_retorna415() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.TEXT_PLAIN)
                            .content("correo=x&contrasena=y"))
                    .andExpect(status().isUnsupportedMediaType());
        }

        @Test
        @DisplayName("ROB-4: método GET sobre /api/auth/login → 405 Method Not Allowed")
        void login_metodoGet_retorna405() throws Exception {
            mockMvc.perform(get("/api/auth/login"))
                    .andExpect(status().isMethodNotAllowed());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  VALIDACIONES DE ENTRADA DEL LOGIN (ya implementadas — deben PASAR)
    //  El login valida campos vacios/en blanco (400) y bloquea por intentos (429).
    //  El formato de correo y la complejidad de contraseña NO se validan aqui:
    //  pertenecen al registro (ver clase RegistroEndpoint), no al login.
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Validaciones de entrada del login")
    class ValidacionesDeEntrada {

        @Test
        @DisplayName("CP-VAL-1: correo vacío \"\" → 400 con mensaje claro")
        void login_correoVacio_retorna400() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("correo", "", "contrasena", CLAVE_OK))))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(Matchers.containsString("correo")));
        }

        @Test
        @DisplayName("CP-VAL-2: contraseña vacía \"\" → 400")
        void login_contrasenaVacia_retorna400() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("correo", CORREO_OK, "contrasena", ""))))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(Matchers.containsString("contraseña")));
        }

        @Test
        @DisplayName("CP-VAL-3: correo solo espacios \"   \" → 400 (isBlank)")
        void login_correoSoloEspacios_retorna400() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("correo", "   ", "contrasena", CLAVE_OK))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("CP-VAL-4: contraseña solo espacios \"   \" → 400 (isBlank)")
        void login_contrasenaSoloEspacios_retorna400() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("correo", CORREO_OK, "contrasena", "   "))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("CP-47: el intento fallido número 10 activa el bloqueo temporal con 429")
        void login_diezIntentosFallidos_bloquea() throws Exception {
            when(authService.login(any(), any())).thenReturn(Optional.empty());

            Map<String, String> credsMalas = new HashMap<>();
            credsMalas.put("correo", CORREO_OK);
            credsMalas.put("contrasena", "claveErrada");

            for (int i = 0; i < 9; i++) {
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json(credsMalas)))
                        .andExpect(status().isUnauthorized());
            }

            // 10º intento fallido: activa el bloqueo temporal.
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(credsMalas)))
                    .andExpect(status().isTooManyRequests())
                    .andExpect(content().string(Matchers.containsString("Espere 15 minutos")));
        }

        @Test
        @DisplayName("CP-47b: un login exitoso reinicia el contador de intentos")
        void login_exitoReiniciaContador() throws Exception {
            // 4 fallos
            when(authService.login(eq(CORREO_OK), eq("mala"))).thenReturn(Optional.empty());
            for (int i = 0; i < 4; i++) {
                mockMvc.perform(post("/api/auth/login")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(json(Map.of("correo", CORREO_OK, "contrasena", "mala"))))
                        .andExpect(status().isUnauthorized());
            }
            // login correcto reinicia el contador
            when(authService.login(eq(CORREO_OK), eq(CLAVE_OK))).thenReturn(Optional.of(usuarioMock));
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("correo", CORREO_OK, "contrasena", CLAVE_OK))))
                    .andExpect(status().isOk());
            // tras el éxito, vuelve a permitir intentos (no bloqueado)
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of("correo", CORREO_OK, "contrasena", "mala"))))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ════════════════════════════════════════════════════════════════════════
    //  REGISTRO DE USUARIO — POST /api/auth/registro
    //  Aqui SI se validan formato de correo y complejidad de contraseña, porque
    //  es el momento en que el usuario las elige (no en el login).
    // ════════════════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Registro de usuario /api/auth/registro")
    class RegistroEndpoint {

        @Test
        @DisplayName("Registro sin token → 401 y no modifica usuarios")
        void registro_sinToken_retorna401() throws Exception {
            mockMvc.perform(post("/api/auth/registro")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "nombre", "X",
                                    "correo", "x@emplanorte.com",
                                    "contrasena", "Admin2024*"))))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("AUTENTICACION_REQUERIDA"));
            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("Registro válido → 201 con datos del usuario (sin hash)")
        void registro_valido_retorna201() throws Exception {
            Usuario creado = new Usuario();
            creado.setId(2L);
            creado.setNombre("Nuevo Admin");
            creado.setCorreo("nuevo@emplanorte.com");
            creado.setRol("administrador");
            creado.setActivo(true);
            when(authService.registrar(any(), any(), any(), any())).thenReturn(creado);

            mockMvc.perform(post("/api/auth/registro")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer token-admin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "nombre", "Nuevo Admin",
                                    "correo", "nuevo@emplanorte.com",
                                    "contrasena", "Admin2024*"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(2))
                    .andExpect(jsonPath("$.correo").value("nuevo@emplanorte.com"))
                    .andExpect(jsonPath("$.contrasenaHash").doesNotExist());
        }

        @Test
        @DisplayName("CP-VAL-5: correo sin formato válido \"noesuncorreo\" → 400 'formato'")
        void registro_correoFormatoInvalido_retorna400() throws Exception {
            mockMvc.perform(post("/api/auth/registro")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer token-admin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "nombre", "X", "correo", "noesuncorreo", "contrasena", "Admin2024*"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(Matchers.containsString("formato")));
            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("CP-VAL-6: correo con espacios internos \"a b@c.com\" → 400")
        void registro_correoConEspacios_retorna400() throws Exception {
            mockMvc.perform(post("/api/auth/registro")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer token-admin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "nombre", "X", "correo", "a b@c.com", "contrasena", "Admin2024*"))))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("CP-VAL-7: contraseña sin mayúscula \"admin2024*\" → 400 'mayúscula'")
        void registro_contrasenaSinMayuscula_retorna400() throws Exception {
            mockMvc.perform(post("/api/auth/registro")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer token-admin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "nombre", "X", "correo", "x@emplanorte.com", "contrasena", "admin2024*"))))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string(Matchers.containsString("mayúscula")));
            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("CP-VAL-8: contraseña muy corta \"Ab1\" → 400 (mínimo de caracteres)")
        void registro_contrasenaCorta_retorna400() throws Exception {
            mockMvc.perform(post("/api/auth/registro")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer token-admin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "nombre", "X", "correo", "x@emplanorte.com", "contrasena", "Ab1"))))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("Campos obligatorios faltantes → 400")
        void registro_camposFaltantes_retorna400() throws Exception {
            mockMvc.perform(post("/api/auth/registro")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer token-admin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().isBadRequest());
            verifyNoInteractions(authService);
        }

        @Test
        @DisplayName("Correo ya registrado → 409 Conflict con mensaje")
        void registro_correoDuplicado_retorna409() throws Exception {
            when(authService.registrar(any(), any(), any(), any()))
                    .thenThrow(new RuntimeException("El correo ya está registrado"));

            mockMvc.perform(post("/api/auth/registro")
                            .header(HttpHeaders.AUTHORIZATION, "Bearer token-admin")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json(Map.of(
                                    "nombre", "X", "correo", "dup@emplanorte.com", "contrasena", "Admin2024*"))))
                    .andExpect(status().isConflict())
                    .andExpect(content().string(Matchers.containsString("ya está registrado")));
        }
    }
}
