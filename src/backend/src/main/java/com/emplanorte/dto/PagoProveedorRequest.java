package com.emplanorte.dto;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
@Data @NoArgsConstructor @AllArgsConstructor
public class PagoProveedorRequest {
    private Long idUsuario;
    private LocalDateTime fechaPago;
    private BigDecimal monto;
    private String metodoPago;
    private String referencia;
    private String observaciones;
}
