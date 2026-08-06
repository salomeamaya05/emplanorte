package com.emplanorte.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VentaRequest {
    private Long idCliente; // Opcional (null para venta directa)
    private Long idUsuario; // Obligatorio
    private LocalDateTime fechaVenta; // Fecha real del negocio; si falta, se usa hora de Bogotá
    private String metodoPago; // efectivo, transferencia, tarjeta, credito, otro
    private BigDecimal descuento;
    private String observaciones;
    private List<ItemVentaRequest> detalles;

    // Solo se usa al crear una venta que reemplaza una venta anulada.
    private Long idVentaOrigen;
    private String motivoCorreccion;
}
