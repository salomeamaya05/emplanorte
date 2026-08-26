package com.emplanorte.dto;

import com.emplanorte.model.AbonoCredito;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AbonoCreditoResponse {
    private Long id;
    private BigDecimal monto;
    private String formaPago;
    private LocalDateTime fechaPago;
    private String tipo;
    private String observaciones;
    private Long idUsuario;
    private String usuarioNombre;
    private LocalDateTime creadoEn;

    public static AbonoCreditoResponse desde(AbonoCredito abono) {
        AbonoCreditoResponse response = new AbonoCreditoResponse();
        response.setId(abono.getId());
        response.setMonto(abono.getMonto());
        response.setFormaPago(abono.getFormaPago());
        response.setFechaPago(abono.getFechaPago());
        response.setTipo(abono.getTipo());
        response.setObservaciones(abono.getObservaciones());
        response.setIdUsuario(abono.getUsuario() == null ? null : abono.getUsuario().getId());
        response.setUsuarioNombre(abono.getUsuario() == null ? null : abono.getUsuario().getNombre());
        response.setCreadoEn(abono.getCreadoEn());
        return response;
    }
}
