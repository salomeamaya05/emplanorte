package com.emplanorte.dto;

import com.emplanorte.model.Cliente;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ClienteCarteraResumenResponse {
    private Cliente cliente;
    private BigDecimal ventasRealizadas;
    private BigDecimal totalPagado;
    private BigDecimal saldoPendiente;
    private Long creditosVencidos;
    private List<CreditoVentaResponse> creditos = new ArrayList<>();
}
