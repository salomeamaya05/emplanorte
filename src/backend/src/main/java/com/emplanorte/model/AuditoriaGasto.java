package com.emplanorte.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Bitácora de auditoría de gastos: guarda un snapshot inmutable de cada
 * creación/edición de un gasto, registrando quién la hizo y cuándo.
 */
@Entity
@Table(name = "auditoria_gastos")
@Data
public class AuditoriaGasto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_gasto", nullable = false)
    private Long idGasto;

    @Column(name = "id_usuario")
    private Long idUsuario;

    @Column(name = "usuario_nombre", length = 150)
    private String usuarioNombre;

    @Column(nullable = false, length = 20)
    private String accion; // 'creacion' | 'edicion'

    @Column(name = "categoria_nombre", length = 100)
    private String categoriaNombre;

    @Column(length = 250)
    private String descripcion;

    private BigDecimal valor;

    @Column(name = "fecha_gasto")
    private LocalDate fechaGasto;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @PrePersist
    void prePersist() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }
    }
}
