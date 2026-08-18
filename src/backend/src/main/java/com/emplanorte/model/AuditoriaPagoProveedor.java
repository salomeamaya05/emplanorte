package com.emplanorte.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria_pagos_proveedores")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditoriaPagoProveedor {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "id_pago", nullable = false) private Long idPago;
    @Column(name = "id_factura", nullable = false) private Long idFactura;
    @Column(name = "id_usuario") private Long idUsuario;
    @Column(name = "usuario_nombre", length = 150) private String usuarioNombre;
    @Column(nullable = false, length = 30) private String accion;
    private BigDecimal monto;
    @Column(columnDefinition = "TEXT") private String motivo;
    @Column(name = "fecha_registro", nullable = false) private LocalDateTime fechaRegistro;
    @PrePersist void prePersist(){ if(fechaRegistro==null) fechaRegistro=LocalDateTime.now(); }
}
