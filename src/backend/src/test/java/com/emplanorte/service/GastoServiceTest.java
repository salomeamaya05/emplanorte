package com.emplanorte.service;

import com.emplanorte.model.CategoriaGasto;
import com.emplanorte.model.Gasto;
import com.emplanorte.repository.CategoriaGastoRepository;
import com.emplanorte.repository.GastoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GastoService — RF09, RF10")
class GastoServiceTest {

    @Mock private GastoRepository gastoRepository;
    @Mock private CategoriaGastoRepository categoriaGastoRepository;

    @InjectMocks private GastoService gastoService;

    private CategoriaGasto categoriaMock;
    private Gasto gastoBase;

    @BeforeEach
    void setUp() {
        categoriaMock = new CategoriaGasto();
        categoriaMock.setId(1L);
        categoriaMock.setNombre("Transporte");
        categoriaMock.setActivo(true);

        gastoBase = new Gasto();
        gastoBase.setId(1L);
        gastoBase.setDescripcion("Gasolina recorrido norte");
        gastoBase.setValor(new BigDecimal("50000"));
        gastoBase.setCategoria(categoriaMock);
        gastoBase.setFechaGasto(LocalDate.now());
    }

    // ─── RF09  Registro de gastos ─────────────────────────────────────────────

    @Test
    @DisplayName("CP-27: Guardar gasto con todos los datos — almacenado y retornado")
    void guardar_gastoCompleto_almacenadoCorrectamente() {
        when(gastoRepository.save(any(Gasto.class))).thenAnswer(inv -> inv.getArgument(0));

        Gasto resultado = gastoService.guardar(gastoBase);

        assertThat(resultado.getDescripcion()).isEqualTo("Gasolina recorrido norte");
        assertThat(resultado.getValor()).isEqualByComparingTo(new BigDecimal("50000"));
        assertThat(resultado.getCategoria().getNombre()).isEqualTo("Transporte");
        verify(gastoRepository).save(gastoBase);
    }

    @Test
    @DisplayName("CP-27b: Gasto sin fecha — se asigna la fecha de hoy automáticamente")
    void guardar_gastoSinFecha_asignaFechaHoy() {
        Gasto sinFecha = new Gasto();
        sinFecha.setDescripcion("Papelería");
        sinFecha.setValor(new BigDecimal("15000"));
        sinFecha.setFechaGasto(null);
        when(gastoRepository.save(any(Gasto.class))).thenAnswer(inv -> inv.getArgument(0));

        Gasto resultado = gastoService.guardar(sinFecha);

        assertThat(resultado.getFechaGasto()).isEqualTo(LocalDate.now());
    }

    @Test
    @DisplayName("CP-27c: Gasto con fecha explícita — respeta la fecha indicada")
    void guardar_gastoConFechaExplicita_respetaFecha() {
        LocalDate fechaAyer = LocalDate.now().minusDays(1);
        gastoBase.setFechaGasto(fechaAyer);
        when(gastoRepository.save(any(Gasto.class))).thenAnswer(inv -> inv.getArgument(0));

        Gasto resultado = gastoService.guardar(gastoBase);

        assertThat(resultado.getFechaGasto()).isEqualTo(fechaAyer);
    }

    @Test
    @DisplayName("CP-33/RF09: Listar todos los gastos — retorna la lista completa")
    void obtenerTodos_retornaListaCompleta() {
        when(gastoRepository.findAll()).thenReturn(List.of(gastoBase));

        List<Gasto> resultado = gastoService.obtenerTodos();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getDescripcion()).isEqualTo("Gasolina recorrido norte");
    }

    @Test
    @DisplayName("CP-34: Sin gastos en el periodo — retorna lista vacía sin error")
    void obtenerTodos_sinGastos_retornaVacioSinError() {
        when(gastoRepository.findAll()).thenReturn(Collections.emptyList());

        List<Gasto> resultado = gastoService.obtenerTodos();

        assertThat(resultado).isEmpty();
    }

    // ─── RF09  Filtro por rango de fechas ─────────────────────────────────────

    @Test
    @DisplayName("CP-36/RF09: Filtrar gastos por rango de fechas — retorna solo los del periodo")
    void obtenerPorFechas_rangoValido_retornaGastosDelPeriodo() {
        LocalDate desde = LocalDate.of(2026, 6, 1);
        LocalDate hasta = LocalDate.of(2026, 6, 30);
        when(gastoRepository.findByFechaGastoBetween(desde, hasta)).thenReturn(List.of(gastoBase));

        List<Gasto> resultado = gastoService.obtenerPorFechas(desde, hasta);

        assertThat(resultado).hasSize(1);
        verify(gastoRepository).findByFechaGastoBetween(desde, hasta);
    }

    @Test
    @DisplayName("CP-38: Rango de fechas sin gastos — lista vacía sin error")
    void obtenerPorFechas_sinResultados_retornaVacioSinError() {
        LocalDate desde = LocalDate.of(2020, 1, 1);
        LocalDate hasta = LocalDate.of(2020, 1, 31);
        when(gastoRepository.findByFechaGastoBetween(desde, hasta)).thenReturn(Collections.emptyList());

        List<Gasto> resultado = gastoService.obtenerPorFechas(desde, hasta);

        assertThat(resultado).isEmpty();
    }

    // ─── RF10  Categorías de gasto ────────────────────────────────────────────

    @Test
    @DisplayName("CP-30: Crear categoría nueva — queda activa y persistida")
    void guardarCategoria_nueva_quedaActivaYPersistida() {
        CategoriaGasto nueva = new CategoriaGasto();
        nueva.setNombre("Servicios públicos");
        nueva.setDescripcion("Agua, luz, internet");
        when(categoriaGastoRepository.save(any(CategoriaGasto.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoriaGasto resultado = gastoService.guardarCategoria(nueva);

        assertThat(resultado.getActivo()).isTrue();
        assertThat(resultado.getNombre()).isEqualTo("Servicios públicos");
        verify(categoriaGastoRepository).save(nueva);
    }

    @Test
    @DisplayName("CP-30b: Listar categorías activas — solo retorna las activas")
    void obtenerCategoriasActivas_soloRetornaActivas() {
        when(categoriaGastoRepository.findByActivoTrue()).thenReturn(List.of(categoriaMock));

        List<CategoriaGasto> resultado = gastoService.obtenerCategoriasActivas();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getNombre()).isEqualTo("Transporte");
    }

    // ─── Particiones de equivalencia ─────────────────────────────────────────

    @Test
    @DisplayName("Partición: valor del gasto es BigDecimal — se almacena con precisión exacta")
    void guardar_valorDecimal_sinPerdidaDePrecision() {
        gastoBase.setValor(new BigDecimal("125000.75"));
        when(gastoRepository.save(any(Gasto.class))).thenAnswer(inv -> inv.getArgument(0));

        Gasto resultado = gastoService.guardar(gastoBase);

        assertThat(resultado.getValor()).isEqualByComparingTo(new BigDecimal("125000.75"));
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PRUEBAS ADICIONALES (rol tester) — documentan cada respuesta del sistema
    // ════════════════════════════════════════════════════════════════════════

    // ── Comportamiento real adicional (deben PASAR) ──────────────────────────

    @Test
    @DisplayName("actualizarGasto existente — los campos se actualizan y se persiste")
    void actualizarGasto_existente_camposActualizados() {
        Gasto cambios = new Gasto();
        cambios.setDescripcion("Gasolina + peajes");
        cambios.setValor(new BigDecimal("65000"));
        cambios.setCategoria(categoriaMock);
        cambios.setFechaGasto(LocalDate.now());

        when(gastoRepository.findById(1L)).thenReturn(java.util.Optional.of(gastoBase));
        when(gastoRepository.save(any(Gasto.class))).thenAnswer(inv -> inv.getArgument(0));

        Gasto resultado = gastoService.actualizarGasto(1L, cambios);

        assertThat(resultado.getDescripcion()).isEqualTo("Gasolina + peajes");
        assertThat(resultado.getValor()).isEqualByComparingTo(new BigDecimal("65000"));
    }

    @Test
    @DisplayName("actualizarGasto inexistente — RuntimeException 'Gasto no encontrado'")
    void actualizarGasto_inexistente_lanzaExcepcion() {
        when(gastoRepository.findById(99L)).thenReturn(java.util.Optional.empty());

        assertThatThrownBy(() -> gastoService.actualizarGasto(99L, gastoBase))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Gasto no encontrado");
    }

    @Test
    @DisplayName("guardarCategoria fuerza activo=true aunque la categoría llegue con activo=false")
    void guardarCategoria_forzaActivoTrue() {
        CategoriaGasto nueva = new CategoriaGasto();
        nueva.setNombre("Impuestos");
        nueva.setActivo(false);
        when(categoriaGastoRepository.save(any(CategoriaGasto.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoriaGasto resultado = gastoService.guardarCategoria(nueva);

        assertThat(resultado.getActivo()).isTrue();
    }

    @Test
    @DisplayName("Listar categorías activas sin resultados — lista vacía sin error")
    void obtenerCategoriasActivas_vacio_sinError() {
        when(categoriaGastoRepository.findByActivoTrue()).thenReturn(Collections.emptyList());

        assertThat(gastoService.obtenerCategoriasActivas()).isEmpty();
    }

    // ── [GAP] Validaciones del Plan NO implementadas (se esperan en ROJO) ─────
    //    GastoService.guardar() solo asigna fecha=hoy si es null; no valida nada.

    @Test
    @DisplayName("[GAP] CP-28: gasto con fecha FUTURA debería rechazarse (hoy se acepta)")
    void guardar_fechaFutura_deberiaRechazar() {
        gastoBase.setFechaGasto(LocalDate.now().plusDays(7));

        assertThatThrownBy(() -> gastoService.guardar(gastoBase))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("futura");
    }

    @Test
    @DisplayName("[GAP] CP-29: gasto con valor CERO debería rechazarse (hoy se acepta)")
    void guardar_valorCero_deberiaRechazar() {
        gastoBase.setValor(BigDecimal.ZERO);

        assertThatThrownBy(() -> gastoService.guardar(gastoBase))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("mayor a cero");
    }

    @Test
    @DisplayName("[GAP] CP-29b: gasto con valor NEGATIVO (-100) debería rechazarse (hoy se acepta)")
    void guardar_valorNegativo_deberiaRechazar() {
        gastoBase.setValor(new BigDecimal("-100"));

        assertThatThrownBy(() -> gastoService.guardar(gastoBase))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("mayor a cero");
    }

    @Test
    @DisplayName("CP-31: categoría duplicada se rechaza")
    void guardarCategoria_duplicada_deberiaRechazar() {
        // Ya existe una categoría activa con ese nombre
        when(categoriaGastoRepository.findByActivoTrue()).thenReturn(List.of(categoriaMock));
        CategoriaGasto duplicada = new CategoriaGasto();
        duplicada.setNombre("Transporte"); // ya existe (categoriaMock)

        assertThatThrownBy(() -> gastoService.guardarCategoria(duplicada))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("ya existe");
        verify(categoriaGastoRepository, never()).save(any());
    }
}
