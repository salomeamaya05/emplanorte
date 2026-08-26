package com.emplanorte.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CarteraResumenResponse {
    private BigDecimal totalPorCobrar;
    private BigDecimal totalVencido;
    private BigDecimal totalPorVencer;
    private Long creditosPendientes;
    private Long creditosVencidos;
    private Long vencenHoy;
    private Long clientesConSaldo;
}
