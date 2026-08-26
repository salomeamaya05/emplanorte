package com.emplanorte.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "abonos_credito",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_abonos_credito_idempotencia",
                columnNames = "clave_idempotencia"
        ),
        indexes = @Index(name = "idx_abonos_credito_fecha", columnList = "id_credito,fecha_pago")
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AbonoCredito {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_credito", nullable = false)
    private CreditoVenta credito;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal monto;

    @Column(name = "forma_pago", nullable = false, length = 30)
    private String formaPago;

    @Column(name = "fecha_pago", nullable = false)
    private LocalDateTime fechaPago;

    /** inicial o abono */
    @Column(nullable = false, length = 20)
    private String tipo;

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Column(name = "clave_idempotencia", nullable = false, length = 80)
    private String claveIdempotencia;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @PrePersist
    void prePersist() {
        if (creadoEn == null) creadoEn = LocalDateTime.now();
        if (tipo == null) tipo = "abono";
    }
}
