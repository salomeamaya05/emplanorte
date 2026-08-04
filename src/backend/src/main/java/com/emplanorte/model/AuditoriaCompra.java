package com.emplanorte.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "auditoria_compras")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuditoriaCompra {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "id_compra", nullable = false) private Long idCompra;
    @Column(name = "id_usuario") private Long idUsuario;
    @Column(name = "usuario_nombre", length = 150) private String usuarioNombre;
    @Column(nullable = false, length = 30) private String accion;
    @Column(name = "numero_compra", length = 30) private String numeroCompra;
    private BigDecimal total;
    @Column(length = 20) private String estado;
    @Column(columnDefinition = "TEXT") private String motivo;
    @Column(name = "fecha_registro", nullable = false) private LocalDateTime fechaRegistro;
    @PrePersist void prePersist(){ if(fechaRegistro==null) fechaRegistro=LocalDateTime.now(); }
}
