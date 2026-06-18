package com.emplanorte.service;

import com.emplanorte.dto.DashboardResponse;
import com.emplanorte.repository.GastoRepository;
import com.emplanorte.repository.VentaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Service
public class DashboardService {

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private GastoRepository gastoRepository;

    public DashboardResponse obtenerResumenFinanciero(LocalDate desde, LocalDate hasta) {
        if (desde == null) {
            desde = LocalDate.now();
        }
        if (hasta == null) {
            hasta = LocalDate.now();
        }

        // Convertir LocalDate a LocalDateTime para la consulta de Ventas
        LocalDateTime startDateTime = desde.atStartOfDay();
        LocalDateTime endDateTime = hasta.atTime(LocalTime.MAX);

        // Obtener sumatorias
        BigDecimal totalVentas = ventaRepository.obtenerTotalVentasPorRango(startDateTime, endDateTime, "completada");
        BigDecimal gananciasVentas = ventaRepository.obtenerGananciaVentasPorRango(startDateTime, endDateTime, "completada");
        BigDecimal totalGastos = gastoRepository.obtenerTotalGastosPorRango(desde, hasta);

        // Calcular utilidad neta real
        BigDecimal gananciaNetaReal = gananciasVentas.subtract(totalGastos);

        return new DashboardResponse(totalVentas, totalGastos, gananciasVentas, gananciaNetaReal);
    }
}
