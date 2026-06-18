package com.emplanorte.service;

import com.emplanorte.dto.DashboardResponse;
import com.emplanorte.repository.GastoRepository;
import com.emplanorte.repository.VentaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("DashboardService — RF11, RF12")
class DashboardServiceTest {

    @Mock private VentaRepository ventaRepository;
    @Mock private GastoRepository gastoRepository;

    @InjectMocks private DashboardService dashboardService;

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private void stubRepositorios(BigDecimal totalVentas, BigDecimal ganancia, BigDecimal totalGastos) {
        when(ventaRepository.obtenerTotalVentasPorRango(any(), any(), eq("completada")))
                .thenReturn(totalVentas);
        when(ventaRepository.obtenerGananciaVentasPorRango(any(), any(), eq("completada")))
                .thenReturn(ganancia);
        when(gastoRepository.obtenerTotalGastosPorRango(any(), any()))
                .thenReturn(totalGastos);
    }

    // ─── RF11  Resumen financiero diario ─────────────────────────────────────

    @Test
    @DisplayName("CP-33: Resumen con ventas y gastos — totales y ganancia neta correctos")
    void obtenerResumen_conOperaciones_calculaTodosLosTotales() {
        stubRepositorios(
                new BigDecimal("500000"),  // total ventas
                new BigDecimal("200000"),  // ganancia ventas
                new BigDecimal("80000")    // total gastos
        );

        DashboardResponse resp = dashboardService.obtenerResumenFinanciero(
                LocalDate.now(), LocalDate.now());

        assertThat(resp.getTotalVentas()).isEqualByComparingTo(new BigDecimal("500000"));
        assertThat(resp.getTotalGastos()).isEqualByComparingTo(new BigDecimal("80000"));
        assertThat(resp.getGananciasVentas()).isEqualByComparingTo(new BigDecimal("200000"));
        assertThat(resp.getGananciaNetaReal()).isEqualByComparingTo(new BigDecimal("120000")); // 200000 - 80000
    }

    @Test
    @DisplayName("CP-34: Día sin operaciones — todos los totales en cero, sin error")
    void obtenerResumen_sinOperaciones_retornaCerosSinError() {
        stubRepositorios(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        DashboardResponse resp = dashboardService.obtenerResumenFinanciero(
                LocalDate.now(), LocalDate.now());

        assertThat(resp.getTotalVentas()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resp.getTotalGastos()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resp.getGananciaNetaReal()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resp).isNotNull();
    }

    @Test
    @DisplayName("CP-33b: Ganancia neta = ganancias ventas − total gastos")
    void obtenerResumen_calculaGananciaNetaCorrectamente() {
        stubRepositorios(
                new BigDecimal("1000000"),
                new BigDecimal("300000"),
                new BigDecimal("150000")
        );

        DashboardResponse resp = dashboardService.obtenerResumenFinanciero(
                LocalDate.now(), LocalDate.now());

        BigDecimal gananciaEsperada = new BigDecimal("300000").subtract(new BigDecimal("150000"));
        assertThat(resp.getGananciaNetaReal()).isEqualByComparingTo(gananciaEsperada);
    }

    @Test
    @DisplayName("CP-33c: Gastos mayores a ganancias — ganancia neta puede ser negativa")
    void obtenerResumen_gastosSuperiores_gananciaNetaNegativa() {
        stubRepositorios(
                new BigDecimal("100000"),
                new BigDecimal("50000"),   // ganancia ventas
                new BigDecimal("200000")   // gastos > ganancias
        );

        DashboardResponse resp = dashboardService.obtenerResumenFinanciero(
                LocalDate.now(), LocalDate.now());

        assertThat(resp.getGananciaNetaReal()).isNegative();
        assertThat(resp.getGananciaNetaReal()).isEqualByComparingTo(new BigDecimal("-150000"));
    }

    // ─── RF12  Estadísticas por rango de fechas ───────────────────────────────

    @Test
    @DisplayName("CP-36: Resumen por rango de fechas — se consulta el periodo correcto")
    void obtenerResumen_porRangoFechas_consultaPeriodoCorrecto() {
        LocalDate desde = LocalDate.of(2026, 5, 1);
        LocalDate hasta = LocalDate.of(2026, 5, 31);
        stubRepositorios(new BigDecimal("800000"), new BigDecimal("300000"), new BigDecimal("100000"));

        dashboardService.obtenerResumenFinanciero(desde, hasta);

        // Verifica que las consultas se hacen con los extremos correctos del día
        LocalDateTime startEsperado = desde.atStartOfDay();
        LocalDateTime endEsperado   = hasta.atTime(LocalTime.MAX);

        verify(ventaRepository).obtenerTotalVentasPorRango(
                eq(startEsperado), eq(endEsperado), eq("completada"));
        verify(gastoRepository).obtenerTotalGastosPorRango(eq(desde), eq(hasta));
    }

    @Test
    @DisplayName("CP-38: Rango sin operaciones — todos los campos en cero, respuesta valida")
    void obtenerResumen_rangoPeriodoVacio_retornaCerosSinError() {
        stubRepositorios(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        DashboardResponse resp = dashboardService.obtenerResumenFinanciero(
                LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31));

        assertThat(resp).isNotNull();
        assertThat(resp.getTotalVentas()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resp.getGananciaNetaReal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ─── Fechas nulas (valor por defecto = hoy) ───────────────────────────────

    @Test
    @DisplayName("CP-37b: Fechas nulas — el servicio usa la fecha de hoy como defecto")
    void obtenerResumen_fechasNulas_usaFechaHoy() {
        stubRepositorios(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        assertThatCode(() -> dashboardService.obtenerResumenFinanciero(null, null))
                .doesNotThrowAnyException();

        // Verifica que la consulta se hace con los límites del día de hoy
        LocalDateTime hoyInicio = LocalDate.now().atStartOfDay();
        LocalDateTime hoyFin    = LocalDate.now().atTime(LocalTime.MAX);
        verify(ventaRepository).obtenerTotalVentasPorRango(
                eq(hoyInicio), eq(hoyFin), eq("completada"));
    }

    @Test
    @DisplayName("Respuesta siempre tiene los 4 campos — nunca retorna null")
    void obtenerResumen_siempreRetorna4Campos() {
        stubRepositorios(new BigDecimal("200000"), new BigDecimal("80000"), new BigDecimal("30000"));

        DashboardResponse resp = dashboardService.obtenerResumenFinanciero(
                LocalDate.now(), LocalDate.now());

        assertThat(resp.getTotalVentas()).isNotNull();
        assertThat(resp.getTotalGastos()).isNotNull();
        assertThat(resp.getGananciasVentas()).isNotNull();
        assertThat(resp.getGananciaNetaReal()).isNotNull();
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PRUEBAS ADICIONALES (rol tester) — documentan cada respuesta del sistema
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Solo 'desde' nula — se usa hoy como inicio y se respeta el 'hasta' indicado")
    void obtenerResumen_soloDesdeNula_usaHoyComoInicio() {
        LocalDate hasta = LocalDate.of(2026, 5, 31);
        stubRepositorios(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        dashboardService.obtenerResumenFinanciero(null, hasta);

        LocalDateTime hoyInicio = LocalDate.now().atStartOfDay();
        LocalDateTime hastaFin  = hasta.atTime(LocalTime.MAX);
        verify(ventaRepository).obtenerTotalVentasPorRango(eq(hoyInicio), eq(hastaFin), eq("completada"));
    }

    @Test
    @DisplayName("Solo 'hasta' nula — se usa hoy como fin y se respeta el 'desde' indicado")
    void obtenerResumen_soloHastaNula_usaHoyComoFin() {
        LocalDate desde = LocalDate.of(2026, 5, 1);
        stubRepositorios(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO);

        dashboardService.obtenerResumenFinanciero(desde, null);

        LocalDateTime desdeInicio = desde.atStartOfDay();
        LocalDateTime hoyFin      = LocalDate.now().atTime(LocalTime.MAX);
        verify(ventaRepository).obtenerTotalVentasPorRango(eq(desdeInicio), eq(hoyFin), eq("completada"));
    }

    @Test
    @DisplayName("La ganancia neta se basa en la GANANCIA de ventas, no en el total facturado")
    void obtenerResumen_gananciaNetaUsaGananciaNoTotalVentas() {
        // total facturado alto (1.000.000) pero ganancia real baja (100.000)
        stubRepositorios(new BigDecimal("1000000"), new BigDecimal("100000"), new BigDecimal("40000"));

        DashboardResponse resp = dashboardService.obtenerResumenFinanciero(
                LocalDate.now(), LocalDate.now());

        // neta = 100.000 - 40.000 = 60.000  (NO 1.000.000 - 40.000)
        assertThat(resp.getGananciaNetaReal()).isEqualByComparingTo(new BigDecimal("60000"));
    }

    @Test
    @DisplayName("Totales con decimales — se conservan sin pérdida de precisión")
    void obtenerResumen_decimales_sinPerdidaDePrecision() {
        stubRepositorios(new BigDecimal("125000.75"), new BigDecimal("50000.25"), new BigDecimal("10000.10"));

        DashboardResponse resp = dashboardService.obtenerResumenFinanciero(
                LocalDate.now(), LocalDate.now());

        assertThat(resp.getGananciaNetaReal()).isEqualByComparingTo(new BigDecimal("40000.15"));
    }
}
