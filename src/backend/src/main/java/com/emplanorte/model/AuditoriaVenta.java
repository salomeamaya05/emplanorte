package com.emplanorte.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Bitácora de auditoría de ventas: registra cada creación y anulación de una
 * venta, dejando rastro de quién la ejecutó y cuándo.
 */
@Entity
@Table(name = "auditoria_ventas")
@Data
public class AuditoriaVenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_venta", nullable = false)
    private Long idVenta;

    @Column(name = "id_usuario")
    private Long idUsuario;

    @Column(name = "usuario_nombre", length = 150)
    private String usuarioNombre;

    @Column(nullable = false, length = 20)
    private String accion; // 'creacion' | 'anulacion'

    @Column(name = "numero_venta", length = 20)
    private String numeroVenta;

    private BigDecimal total;

    @Column(length = 20)
    private String estado;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @PrePersist
    void prePersist() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }
    }
}
