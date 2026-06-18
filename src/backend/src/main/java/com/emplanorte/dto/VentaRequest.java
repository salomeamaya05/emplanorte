package com.emplanorte.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VentaRequest {
    private Long idCliente; // Opcional (null para venta directa)
    private Long idUsuario; // Obligatorio
    private String metodoPago; // efectivo, transferencia, tarjeta, otro
    private BigDecimal descuento;
    private String observaciones;
    private List<ItemVentaRequest> detalles;
}
