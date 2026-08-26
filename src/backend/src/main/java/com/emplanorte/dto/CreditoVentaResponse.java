package com.emplanorte.dto;

import com.emplanorte.model.Cliente;
import com.emplanorte.model.CreditoVenta;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditoVentaResponse {
    private static final ZoneId ZONA_BOGOTA = ZoneId.of("America/Bogota");
    private Long id;
    private Long idVenta;
    private String numeroVenta;
    private Cliente cliente;
    private BigDecimal totalCredito;
    private BigDecimal totalAbonado;
    private BigDecimal saldoPendiente;
    private LocalDate fechaVenta;
    private LocalDate fechaVencimiento;
    private String estado;
    private Long diasParaVencer;
    private String observaciones;
    private LocalDateTime creadoEn;
    private LocalDateTime actualizadoEn;

    public static CreditoVentaResponse desde(CreditoVenta credito) {
        CreditoVentaResponse response = new CreditoVentaResponse();
        response.setId(credito.getId());
        response.setIdVenta(credito.getVenta().getId());
        response.setNumeroVenta(credito.getVenta().getNumeroVenta());
        response.setCliente(credito.getCliente());
        response.setTotalCredito(credito.getTotalCredito());
        response.setSaldoPendiente(credito.getSaldoPendiente());
        response.setTotalAbonado(credito.getTotalCredito().subtract(credito.getSaldoPendiente()));
        response.setFechaVenta(credito.getVenta().getFechaVenta().toLocalDate());
        response.setFechaVencimiento(credito.getFechaVencimiento());
        response.setEstado(estadoVisible(credito));
        response.setDiasParaVencer(ChronoUnit.DAYS.between(LocalDate.now(ZONA_BOGOTA), credito.getFechaVencimiento()));
        response.setObservaciones(credito.getObservaciones());
        response.setCreadoEn(credito.getCreadoEn());
        response.setActualizadoEn(credito.getActualizadoEn());
        return response;
    }

    private static String estadoVisible(CreditoVenta credito) {
        if ("pagado".equalsIgnoreCase(credito.getEstado())) return "pagado";
        if ("anulado".equalsIgnoreCase(credito.getEstado())) return "anulado";
        LocalDate hoy = LocalDate.now(ZONA_BOGOTA);
        if (credito.getFechaVencimiento().isBefore(hoy)) return "vencido";
        if (credito.getFechaVencimiento().isEqual(hoy)) return "vence_hoy";
        return "pendiente";
    }
}
