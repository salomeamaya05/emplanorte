package com.emplanorte.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pagos_proveedores")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PagoProveedor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_factura", nullable = false)
    private FacturaProveedor factura;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_pago", nullable = false) private LocalDateTime fechaPago;
    @Column(nullable = false) private BigDecimal monto;
    @Column(name = "metodo_pago", nullable = false, length = 30) private String metodoPago;
    @Column(length = 150) private String referencia;
    @Column(columnDefinition = "TEXT") private String observaciones;
    @Column(nullable = false, length = 20) private String estado = "activo";
    @Column(name = "motivo_anulacion", columnDefinition = "TEXT") private String motivoAnulacion;
    @Column(name = "anulado_en") private LocalDateTime anuladoEn;
    @Column(name = "creado_en", nullable = false) private LocalDateTime creadoEn;

    @JsonProperty("idFactura")
    public Long getIdFactura() { return factura == null ? null : factura.getId(); }

    @PrePersist
    void prePersist() {
        LocalDateTime ahora = LocalDateTime.now();
        if (fechaPago == null) fechaPago = ahora;
        if (creadoEn == null) creadoEn = ahora;
        if (estado == null) estado = "activo";
    }
}
