package com.emplanorte.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CotizacionRequest {
    private Long idCliente;
    private Long idUsuario;
    private BigDecimal descuento;
    private String notas;
    private List<ItemCotizacionRequest> detalles;
}
