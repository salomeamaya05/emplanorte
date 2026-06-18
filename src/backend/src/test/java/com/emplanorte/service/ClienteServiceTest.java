package com.emplanorte.service;

import com.emplanorte.model.Cliente;
import com.emplanorte.repository.ClienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClienteService — RF13")
class ClienteServiceTest {

    @Mock private ClienteRepository clienteRepository;
    @InjectMocks private ClienteService clienteService;

    private Cliente clienteBase;

    @BeforeEach
    void setUp() {
        clienteBase = new Cliente();
        clienteBase.setId(1L);
        clienteBase.setNombre("Distribuidora Norte");
        clienteBase.setTelefono("3001234567");
        clienteBase.setDireccion("Calle 10 #20-30, Cúcuta");
        clienteBase.setObservaciones("Cliente frecuente");
        clienteBase.setActivo(true);
    }

    // ─── RF13  Registro de clientes ───────────────────────────────────────────

    @Test
    @DisplayName("CP-39: Registrar cliente con datos mínimos — queda activo y disponible")
    void guardar_clienteNuevo_quedaActivoYPersistido() {
        Cliente nuevo = new Cliente();
        nuevo.setNombre("Juan Esteban Orozco");
        nuevo.setTelefono("3109876543");
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        Cliente resultado = clienteService.guardar(nuevo);

        assertThat(resultado.getActivo()).isTrue();
        assertThat(resultado.getNombre()).isEqualTo("Juan Esteban Orozco");
        verify(clienteRepository).save(nuevo);
    }

    @Test
    @DisplayName("CP-39b: Campos opcionales no impiden el registro")
    void guardar_sinCamposOpcionales_seRegistraCorrectamente() {
        Cliente sinOpcionales = new Cliente();
        sinOpcionales.setNombre("Cliente Básico");
        sinOpcionales.setTelefono("3150000000");
        // sin dirección ni observaciones
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThatCode(() -> clienteService.guardar(sinOpcionales))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("CP-39c: Listar clientes activos — solo retorna activos")
    void obtenerTodosActivos_soloRetornaActivos() {
        when(clienteRepository.findByActivoTrue()).thenReturn(List.of(clienteBase));

        List<Cliente> resultado = clienteService.obtenerTodosActivos();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getActivo()).isTrue();
    }

    @Test
    @DisplayName("Sin clientes registrados — retorna lista vacía sin error")
    void obtenerTodosActivos_sinClientes_retornaVacioSinError() {
        when(clienteRepository.findByActivoTrue()).thenReturn(Collections.emptyList());

        List<Cliente> resultado = clienteService.obtenerTodosActivos();

        assertThat(resultado).isEmpty();
    }

    // ─── Obtener por ID ───────────────────────────────────────────────────────

    @Test
    @DisplayName("Obtener por ID — cliente activo es retornado")
    void obtenerPorId_clienteActivo_retornaCliente() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteBase));

        Optional<Cliente> resultado = clienteService.obtenerPorId(1L);

        assertThat(resultado).isPresent();
        assertThat(resultado.get().getNombre()).isEqualTo("Distribuidora Norte");
    }

    @Test
    @DisplayName("Obtener por ID — cliente inactivo no se retorna")
    void obtenerPorId_clienteInactivo_retornaVacio() {
        clienteBase.setActivo(false);
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteBase));

        Optional<Cliente> resultado = clienteService.obtenerPorId(1L);

        assertThat(resultado).isEmpty();
    }

    // ─── Actualizar cliente ───────────────────────────────────────────────────

    @Test
    @DisplayName("CP-39d: Actualizar cliente existente — campos nuevos quedan guardados")
    void actualizar_clienteExistente_camposActualizados() {
        Cliente cambios = new Cliente();
        cambios.setNombre("Distribuidora Norte S.A.S.");
        cambios.setTelefono("3007654321");
        cambios.setDireccion("Av. 1 #5-20, Cúcuta");
        cambios.setObservaciones("Actualizado");

        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteBase));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        Cliente resultado = clienteService.actualizar(1L, cambios);

        assertThat(resultado.getNombre()).isEqualTo("Distribuidora Norte S.A.S.");
        assertThat(resultado.getTelefono()).isEqualTo("3007654321");
    }

    @Test
    @DisplayName("Actualizar cliente inexistente — RuntimeException con mensaje claro")
    void actualizar_clienteNoExiste_lanzaExcepcion() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.actualizar(99L, clienteBase))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cliente");
    }

    // ─── Desactivar cliente ───────────────────────────────────────────────────

    @Test
    @DisplayName("CP-40: Desactivar cliente — activo pasa a false, no se borra físicamente")
    void desactivar_cliente_marcaInactivoSinBorrar() {
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(clienteBase));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        clienteService.desactivar(1L);

        assertThat(clienteBase.getActivo()).isFalse();
        verify(clienteRepository).save(clienteBase);
        verify(clienteRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("Desactivar cliente inexistente — RuntimeException con mensaje claro")
    void desactivar_clienteNoExiste_lanzaExcepcion() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.desactivar(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cliente");
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PRUEBAS ADICIONALES (rol tester) — documentan cada respuesta del sistema
    // ════════════════════════════════════════════════════════════════════════

    // ── Comportamiento real adicional (deben PASAR) ──────────────────────────

    @Test
    @DisplayName("guardar fuerza activo=true aunque el cliente llegue con activo=false")
    void guardar_forzaActivoTrue() {
        Cliente nuevo = new Cliente();
        nuevo.setNombre("Cliente X");
        nuevo.setTelefono("3001112233");
        nuevo.setActivo(false);
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));

        Cliente resultado = clienteService.guardar(nuevo);

        assertThat(resultado.getActivo()).isTrue();
    }

    @Test
    @DisplayName("actualizar inexistente — mensaje EXACTO 'Cliente no encontrado'")
    void actualizar_inexistente_mensajeExacto() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.actualizar(99L, clienteBase))
                .hasMessage("Cliente no encontrado");
    }

    @Test
    @DisplayName("desactivar inexistente — mensaje EXACTO 'Cliente no encontrado'")
    void desactivar_inexistente_mensajeExacto() {
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.desactivar(99L))
                .hasMessage("Cliente no encontrado");
    }

    // ── [GAP] Validaciones del Plan NO implementadas (se esperan en ROJO) ─────
    //    ClienteService.guardar() solo fuerza activo=true; no valida nombre ni teléfono.

    @Test
    @DisplayName("[GAP] CP-40: cliente sin nombre (\"\") debería rechazarse (hoy se acepta)")
    void guardar_nombreVacio_deberiaRechazar() {
        Cliente sinNombre = new Cliente();
        sinNombre.setNombre("");
        sinNombre.setTelefono("3001234567");

        assertThatThrownBy(() -> clienteService.guardar(sinNombre))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("nombre");
    }

    @Test
    @DisplayName("[GAP] CP-40b: cliente con nombre null debería rechazarse (hoy se acepta)")
    void guardar_nombreNull_deberiaRechazar() {
        Cliente sinNombre = new Cliente();
        sinNombre.setNombre(null);
        sinNombre.setTelefono("3001234567");

        assertThatThrownBy(() -> clienteService.guardar(sinNombre))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("nombre");
    }

    @Test
    @DisplayName("[GAP] CP-41: teléfono con formato inválido (\"abc123\") debería rechazarse (hoy se acepta)")
    void guardar_telefonoInvalido_deberiaRechazar() {
        Cliente telMalo = new Cliente();
        telMalo.setNombre("Cliente Y");
        telMalo.setTelefono("abc123");

        assertThatThrownBy(() -> clienteService.guardar(telMalo))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("formato");
    }
}
