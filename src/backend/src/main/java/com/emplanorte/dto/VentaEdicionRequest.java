package com.emplanorte.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Edición administrativa de una venta ya registrada.
 * No admite productos, cantidades, precios, descuento ni totales.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VentaEdicionRequest {
    private Long idCliente;
    private LocalDateTime fechaVenta;
    private String metodoPago;
    private String observaciones;
    private String motivo;
    private Long idUsuario;
    private String contrasena;
}
