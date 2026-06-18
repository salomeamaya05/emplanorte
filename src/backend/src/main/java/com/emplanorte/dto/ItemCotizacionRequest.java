package com.emplanorte.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemCotizacionRequest {

    private Long idProducto;
    private Integer cantidad;
    private BigDecimal precioUnitario;

    public ItemCotizacionRequest(Long idProducto, Integer cantidad) {
        this.idProducto = idProducto;
        this.cantidad = cantidad;
        this.precioUnitario = null;
    }
}