package com.emplanorte.dto;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
@Data @NoArgsConstructor @AllArgsConstructor
public class CompraRequest {
    private Long idProveedor;
    private Long idUsuario;
    private LocalDateTime fechaCompra;
    private BigDecimal flete;
    private BigDecimal impuestos;
    private BigDecimal descuento;
    private String observaciones;
    private List<ItemCompraRequest> detalles;
    private Boolean registrarFactura;
    private String numeroFactura;
    private LocalDate fechaEmision;
    private LocalDate fechaVencimiento;
    private String observacionesFactura;
}
