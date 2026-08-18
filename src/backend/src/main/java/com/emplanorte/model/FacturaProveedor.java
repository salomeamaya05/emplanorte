package com.emplanorte.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "facturas_proveedores")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FacturaProveedor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_compra", nullable = false, unique = true)
    private Compra compra;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_proveedor", nullable = false)
    private Proveedor proveedor;

    @Column(name = "numero_factura", nullable = false, length = 80) private String numeroFactura;
    @Column(name = "fecha_emision", nullable = false) private LocalDate fechaEmision;
    @Column(name = "fecha_vencimiento") private LocalDate fechaVencimiento;
    @Column(name = "total_factura", nullable = false) private BigDecimal totalFactura;
    @Column(name = "total_pagado", nullable = false) private BigDecimal totalPagado;
    @Column(name = "saldo_pendiente", nullable = false) private BigDecimal saldoPendiente;
    @Column(name = "estado_pago", nullable = false, length = 20) private String estadoPago;
    @Column(name = "ruta_adjunto", columnDefinition = "TEXT") private String rutaAdjunto;
    @Column(name = "nombre_adjunto", length = 255) private String nombreAdjunto;
    @Column(name = "tipo_adjunto", length = 100) private String tipoAdjunto;
    @Column(columnDefinition = "TEXT") private String observaciones;
    @Column(name = "creado_en", nullable = false) private LocalDateTime creadoEn;
    @Column(name = "actualizado_en", nullable = false) private LocalDateTime actualizadoEn;

    @JsonProperty("idCompra")
    public Long getIdCompra() { return compra == null ? null : compra.getId(); }

    @JsonProperty("numeroCompra")
    public String getNumeroCompra() { return compra == null ? null : compra.getNumeroCompra(); }

    @PrePersist
    void prePersist() {
        LocalDateTime ahora = LocalDateTime.now();
        if (creadoEn == null) creadoEn = ahora;
        actualizadoEn = ahora;
        if (fechaEmision == null) fechaEmision = LocalDate.now();
        if (totalPagado == null) totalPagado = BigDecimal.ZERO;
        if (saldoPendiente == null) saldoPendiente = totalFactura;
        if (estadoPago == null) estadoPago = "pendiente";
    }

    @PreUpdate void preUpdate() { actualizadoEn = LocalDateTime.now(); }
}
