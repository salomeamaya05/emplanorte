package com.emplanorte.dto;
import lombok.*;
import java.math.BigDecimal;
@Data @NoArgsConstructor @AllArgsConstructor
public class ItemCompraRequest {
    private Long idProducto;
    private Integer cantidad;
    private Integer cantidadPacas;
    private Integer unidadesPorPaca;
    private BigDecimal costoUnitario;
}
