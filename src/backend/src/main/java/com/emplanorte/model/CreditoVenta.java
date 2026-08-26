package com.emplanorte.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "creditos_venta",
        uniqueConstraints = @UniqueConstraint(name = "uq_creditos_venta", columnNames = "id_venta"),
        indexes = {
                @Index(name = "idx_creditos_estado_vencimiento", columnList = "estado,fecha_vencimiento"),
                @Index(name = "idx_creditos_cliente", columnList = "id_cliente")
        }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditoVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_venta", nullable = false, unique = true)
    private Venta venta;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "id_cliente", nullable = false)
    private Cliente cliente;

    @Column(name = "total_credito", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalCredito;

    @Column(name = "saldo_pendiente", nullable = false, precision = 14, scale = 2)
    private BigDecimal saldoPendiente;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    /** Estado persistido: pendiente, pagado o anulado. Vencido se calcula por fecha. */
    @Column(nullable = false, length = 20)
    private String estado = "pendiente";

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    @Version
    @Column(nullable = false)
    private Long version;

    @Column(name = "creado_en", nullable = false)
    private LocalDateTime creadoEn;

    @Column(name = "actualizado_en", nullable = false)
    private LocalDateTime actualizadoEn;

    @PrePersist
    void prePersist() {
        LocalDateTime ahora = LocalDateTime.now();
        if (creadoEn == null) creadoEn = ahora;
        actualizadoEn = ahora;
        if (estado == null) estado = "pendiente";
        if (version == null) version = 0L;
    }

    @PreUpdate
    void preUpdate() {
        actualizadoEn = LocalDateTime.now();
    }
}
