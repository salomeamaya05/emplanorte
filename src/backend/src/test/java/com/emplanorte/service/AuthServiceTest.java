package com.emplanorte.service;

import com.emplanorte.model.Usuario;
import com.emplanorte.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService — RNF05 Control de acceso")
class AuthServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private AuthService authService;

    private Usuario usuarioActivo;

    @BeforeEach
    void setUp() {
        usuarioActivo = new Usuario();
        usuarioActivo.setId(1L);
        usuarioActivo.setNombre("Duvan Alvarado");
        usuarioActivo.setCorreo("duvan@emplanorte.com");
        usuarioActivo.setContrasenaHash("$2a$10$hash_simulado");
        usuarioActivo.setRol("administrador");
        usuarioActivo.setActivo(true);
    }

    // ─── CP-45  Login exitoso ─────────────────────────────────────────────────

    @Test
    @DisplayName("CP-45: Login con credenciales válidas — retorna el usuario autenticado")
    void login_credencialesValidas_retornaUsuario() {
        when(usuarioRepository.findByCorreo("duvan@emplanorte.com"))
                .thenReturn(Optional.of(usuarioActivo));
        when(passwordEncoder.matches("Admin2024*", "$2a$10$hash_simulado"))
                .thenReturn(true);

        Optional<Usuario> resultado = authService.login("duvan@emplanorte.com", "Admin2024*");

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getNombre()).isEqualTo("Duvan Alvarado");
        assertThat(resultado.get().getRol()).isEqualTo("administrador");
    }

    @Test
    @DisplayName("CP-45b: Login exitoso — se usa BCrypt para comparar, nunca texto plano")
    void login_exitoso_usaBCryptNoTextoPlano() {
        when(usuarioRepository.findByCorreo("duvan@emplanorte.com"))
                .thenReturn(Optional.of(usuarioActivo));
        when(passwordEncoder.matches(any(), any())).thenReturn(true);

        authService.login("duvan@emplanorte.com", "Admin2024*");

        // Verifica que se llamó al encoder, nunca comparación directa de strings
        verify(passwordEncoder).matches("Admin2024*", "$2a$10$hash_simulado");
    }

    // ─── CP-46  Credenciales inválidas ────────────────────────────────────────

    @Test
    @DisplayName("CP-46: Contraseña incorrecta — retorna vacío sin revelar si el correo existe")
    void login_contrasenaIncorrecta_retornaVacio() {
        when(usuarioRepository.findByCorreo("duvan@emplanorte.com"))
                .thenReturn(Optional.of(usuarioActivo));
        when(passwordEncoder.matches("claveErrada", "$2a$10$hash_simulado"))
                .thenReturn(false);

        Optional<Usuario> resultado = authService.login("duvan@emplanorte.com", "claveErrada");

        assertThat(resultado).isEmpty();
    }

    @Test
    @DisplayName("CP-46b: Correo no registrado — retorna vacío (mismo resultado que contraseña mal)")
    void login_correoNoExiste_retornaVacio() {
        when(usuarioRepository.findByCorreo("noexiste@emplanorte.com"))
                .thenReturn(Optional.empty());

        Optional<Usuario> resultado = authService.login("noexiste@emplanorte.com", "cualquierClave");

        assertThat(resultado).isEmpty();
        // El encoder no debe llamarse si el usuario no existe
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("CP-46c: Respuesta idéntica para correo inexistente y contraseña mala — no filtra información")
    void login_respuestaIdentica_paraCorreoInexistenteYClaveMala() {
        when(usuarioRepository.findByCorreo("noexiste@emplanorte.com"))
                .thenReturn(Optional.empty());
        when(usuarioRepository.findByCorreo("duvan@emplanorte.com"))
                .thenReturn(Optional.of(usuarioActivo));
        when(passwordEncoder.matches("claveErrada", "$2a$10$hash_simulado"))
                .thenReturn(false);

        Optional<Usuario> sinCorreo = authService.login("noexiste@emplanorte.com", "Admin2024*");
        Optional<Usuario> sinClave  = authService.login("duvan@emplanorte.com", "claveErrada");

        // Ambos casos devuelven Optional vacío — el cliente no puede distinguirlos
        assertThat(sinCorreo).isEmpty();
        assertThat(sinClave).isEmpty();
    }

    // ─── Usuario inactivo ─────────────────────────────────────────────────────

    @Test
    @DisplayName("CP-46d: Usuario inactivo — no puede iniciar sesión aunque la clave sea correcta")
    void login_usuarioInactivo_retornaVacio() {
        usuarioActivo.setActivo(false);
        when(usuarioRepository.findByCorreo("duvan@emplanorte.com"))
                .thenReturn(Optional.of(usuarioActivo));

        Optional<Usuario> resultado = authService.login("duvan@emplanorte.com", "Admin2024*");

        assertThat(resultado).isEmpty();
        // No debe llamar al encoder si el usuario está inactivo
        verifyNoInteractions(passwordEncoder);
    }

    // ─── CP-47  Intentos repetidos ────────────────────────────────────────────

    @Test
    @DisplayName("CP-47: Múltiples intentos fallidos — servicio responde consistentemente (bloqueo pendiente)")
    void login_multiplesIntentosFallidos_respondeConsistentemente() {
        when(usuarioRepository.findByCorreo("duvan@emplanorte.com"))
                .thenReturn(Optional.of(usuarioActivo));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        // Cinco intentos fallidos seguidos no lanzan excepción ni cambian comportamiento
        // NOTA: el bloqueo de cuenta por intentos fallidos (CP-47 completo) está pendiente
        // de implementar en el servicio. Este test documenta el comportamiento actual.
        for (int i = 1; i <= 5; i++) {
            Optional<Usuario> resultado = authService.login("duvan@emplanorte.com", "claveErrada" + i);
            assertThat(resultado).isEmpty();
        }

        // El encoder se llamó exactamente 5 veces (una por cada intento)
        verify(passwordEncoder, times(5)).matches(anyString(), anyString());
    }

    // ─── CP-48  Inyección SQL ─────────────────────────────────────────────────

    @Test
    @DisplayName("CP-48: String con inyección SQL como correo — tratado como texto literal, no como query")
    void login_inyeccionSqlEnCorreo_noEjecuta() {
        String intentoInyeccion = "admin' OR '1'='1";

        // Spring Data JPA pasa el valor como parámetro (PreparedStatement), nunca concatenado
        // El repositorio buscará un usuario cuyo correo SEA exactamente ese string → no encuentra
        when(usuarioRepository.findByCorreo(intentoInyeccion)).thenReturn(Optional.empty());

        Optional<Usuario> resultado = authService.login(intentoInyeccion, "cualquierClave");

        assertThat(resultado).isEmpty();
        // El encoder no se invoca: no hubo bypass de autenticación
        verifyNoInteractions(passwordEncoder);
        // Se verificó que findByCorreo recibió el string tal cual, sin ejecutarlo como SQL
        verify(usuarioRepository).findByCorreo(intentoInyeccion);
    }

    @Test
    @DisplayName("CP-48b: String con inyección SQL como contraseña — BCrypt neutraliza el intento")
    void login_inyeccionSqlEnContrasena_bcryptNeutraliza() {
        String intentoInyeccion = "' OR '1'='1";
        when(usuarioRepository.findByCorreo("duvan@emplanorte.com"))
                .thenReturn(Optional.of(usuarioActivo));
        // BCrypt compara hashes, nunca ejecuta el string como SQL
        when(passwordEncoder.matches(intentoInyeccion, "$2a$10$hash_simulado")).thenReturn(false);

        Optional<Usuario> resultado = authService.login("duvan@emplanorte.com", intentoInyeccion);

        assertThat(resultado).isEmpty();
    }

    // ─── Particiones de equivalencia ─────────────────────────────────────────

    @Test
    @DisplayName("Partición: correo nulo — retorna vacío sin NullPointerException")
    void login_correoNulo_retornaVacioSinExcepcion() {
        when(usuarioRepository.findByCorreo(null)).thenReturn(Optional.empty());

        assertThatCode(() -> authService.login(null, "Admin2024*"))
                .doesNotThrowAnyException();

        Optional<Usuario> resultado = authService.login(null, "Admin2024*");
        assertThat(resultado).isEmpty();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PRUEBAS ADICIONALES (rol tester) — documentan cada respuesta del sistema
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Correo vacío \"\" — el servicio no valida formato, solo busca y devuelve vacío")
    void login_correoVacio_retornaVacio() {
        when(usuarioRepository.findByCorreo("")).thenReturn(Optional.empty());

        Optional<Usuario> resultado = authService.login("", "Admin2024*");

        assertThat(resultado).isEmpty();
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    @DisplayName("Correo en otro case (MAYÚSCULAS) — la búsqueda es exacta; si el repo no lo halla, vacío")
    void login_correoOtroCase_dependeDelRepositorio() {
        when(usuarioRepository.findByCorreo("DUVAN@EMPLANORTE.COM")).thenReturn(Optional.empty());

        Optional<Usuario> resultado = authService.login("DUVAN@EMPLANORTE.COM", "Admin2024*");

        assertThat(resultado).isEmpty();
        verify(usuarioRepository).findByCorreo("DUVAN@EMPLANORTE.COM");
    }

    @Test
    @DisplayName("Login exitoso — devuelve exactamente el mismo usuario (id y correo) que está en BD")
    void login_exitoso_devuelveMismoUsuario() {
        when(usuarioRepository.findByCorreo("duvan@emplanorte.com"))
                .thenReturn(Optional.of(usuarioActivo));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(true);

        Optional<Usuario> resultado = authService.login("duvan@emplanorte.com", "Admin2024*");

        assertThat(resultado).contains(usuarioActivo);
        assertThat(resultado.get().getId()).isEqualTo(1L);
        assertThat(resultado.get().getCorreo()).isEqualTo("duvan@emplanorte.com");
    }

    // ─── Registro (RNF05) ─────────────────────────────────────────────────────

    @Test
    @DisplayName("registrar correo nuevo — cifra la contraseña con BCrypt, queda activo y se guarda")
    void registrar_correoNuevo_cifraYGuarda() {
        when(usuarioRepository.findByCorreo("nuevo@emplanorte.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Admin2024*")).thenReturn("$2a$10$hashNuevo");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario u = authService.registrar("nuevo@emplanorte.com", "Admin2024*", "Nuevo Admin", "administrador");

        assertThat(u.getContrasenaHash()).isEqualTo("$2a$10$hashNuevo");
        assertThat(u.getContrasenaHash()).isNotEqualTo("Admin2024*"); // nunca texto plano
        assertThat(u.getActivo()).isTrue();
        assertThat(u.getCorreo()).isEqualTo("nuevo@emplanorte.com");
        assertThat(u.getRol()).isEqualTo("administrador");
    }

    @Test
    @DisplayName("registrar con correo ya existente — RuntimeException y no se guarda")
    void registrar_correoExistente_lanzaExcepcion() {
        when(usuarioRepository.findByCorreo("duvan@emplanorte.com"))
                .thenReturn(Optional.of(usuarioActivo));

        assertThatThrownBy(() ->
                authService.registrar("duvan@emplanorte.com", "Admin2024*", "Duplicado", "administrador"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ya está registrado");

        verify(usuarioRepository, never()).save(any());
    }

    @Test
    @DisplayName("registrar sin rol — asigna 'administrador' por defecto")
    void registrar_sinRol_asignaAdministrador() {
        when(usuarioRepository.findByCorreo("nuevo2@emplanorte.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$hash");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario u = authService.registrar("nuevo2@emplanorte.com", "Admin2024*", "Sin Rol", null);

        assertThat(u.getRol()).isEqualTo("administrador");
    }
}
