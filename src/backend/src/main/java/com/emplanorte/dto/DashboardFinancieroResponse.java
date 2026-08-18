package com.emplanorte.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor
public class DashboardFinancieroResponse {
    private LocalDate desde;
    private LocalDate hasta;
    private BigDecimal ventasNetas;
    private Long numeroVentas;
    private BigDecimal comprasPeriodo;
    private BigDecimal gastosOperativos;
    private BigDecimal costoVentas;
    private BigDecimal gananciaBruta;
    private BigDecimal gananciaNeta;
    private BigDecimal inventarioValorizado;
    private BigDecimal cuentasPorPagar;
    private BigDecimal capitalInvertidoAcumulado;
    private BigDecimal capitalRecuperadoAcumulado;
    private Long facturasVencidas;
    private Long facturasPorVencer;
    private Long productosStockBajo;
    private List<SerieMensual> serieMensual = new ArrayList<>();
    private List<ProveedorTop> proveedoresTop = new ArrayList<>();
    private List<AlertaFactura> alertasFacturas = new ArrayList<>();

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class SerieMensual {
        private String periodo;
        private String etiqueta;
        private BigDecimal ventas;
        private BigDecimal compras;
        private BigDecimal gastos;
        private BigDecimal gananciaNeta;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class ProveedorTop {
        private Long idProveedor;
        private String razonSocial;
        private BigDecimal totalComprado;
    }

    @Data @NoArgsConstructor @AllArgsConstructor
    public static class AlertaFactura {
        private Long idFactura;
        private String numeroFactura;
        private String proveedor;
        private LocalDate fechaVencimiento;
        private BigDecimal saldoPendiente;
        private String nivel;
        private Long dias;
    }
}
