package com.emplanorte.dto;
import lombok.*;
import java.time.LocalDate;
@Data @NoArgsConstructor @AllArgsConstructor
public class FacturaProveedorRequest {
    private Long idCompra;
    private String numeroFactura;
    private LocalDate fechaEmision;
    private LocalDate fechaVencimiento;
    private String observaciones;
}
