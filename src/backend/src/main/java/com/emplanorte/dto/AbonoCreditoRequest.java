package com.emplanorte.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AbonoCreditoRequest {
    private BigDecimal monto;
    private String formaPago;
    private LocalDateTime fechaPago;
    private String observaciones;
    private Long idUsuario;
    private String claveIdempotencia;
}
