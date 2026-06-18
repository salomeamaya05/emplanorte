package com.emplanorte.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    private BigDecimal totalVentas;
    private BigDecimal totalGastos;
    private BigDecimal gananciasVentas; // Ganancia bruta generada por las ventas
    private BigDecimal gananciaNetaReal; // gananciasVentas - totalGastos
}
