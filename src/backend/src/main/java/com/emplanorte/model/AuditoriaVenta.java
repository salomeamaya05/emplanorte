package com.emplanorte.model;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Bitácora inmutable de ventas. Registra creación, edición administrativa,
 * anulación y relaciones de corrección sin borrar el historial anterior.
 */
@Entity
@Table(name = "auditoria_ventas")
@Data
public class AuditoriaVenta {

    private static final ZoneId ZONA_BOGOTA = ZoneId.of("America/Bogota");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_venta", nullable = false)
    private Long idVenta;

    @Column(name = "id_usuario")
    private Long idUsuario;

    @Column(name = "usuario_nombre", length = 150)
    private String usuarioNombre;

    @Column(nullable = false, length = 30)
    private String accion;

    @Column(name = "numero_venta", length = 20)
    private String numeroVenta;

    private BigDecimal total;

    @Column(length = 20)
    private String estado;

    @Column(columnDefinition = "TEXT")
    private String motivo;

    @Column(name = "detalle_cambios", columnDefinition = "TEXT")
    private String detalleCambios;

    @Column(name = "id_venta_relacionada")
    private Long idVentaRelacionada;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @PrePersist
    void prePersist() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now(ZONA_BOGOTA);
        }
    }
}
