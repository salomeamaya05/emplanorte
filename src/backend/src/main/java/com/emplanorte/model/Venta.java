package com.emplanorte.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "ventas")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_venta", nullable = false, length = 20, unique = true)
    private String numeroVenta;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_cliente")
    private Cliente cliente;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_venta", nullable = false)
    private LocalDateTime fechaVenta;

    /** Momento real en que el registro fue creado en el sistema. */
    @Column(name = "creado_en", insertable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @Column(nullable = false)
    private BigDecimal subtotal;

    @Column(nullable = false)
    private BigDecimal descuento;

    @Column(nullable = false)
    private BigDecimal total;

    @Column(name = "total_costo", nullable = false)
    private BigDecimal totalCosto;

    @Column(nullable = false)
    private BigDecimal ganancia;

    @Column(name = "metodo_pago", nullable = false, length = 30)
    private String metodoPago;

    @Column(nullable = false, length = 20)
    private String estado = "completada";

    @Column(columnDefinition = "TEXT")
    private String observaciones;

    /** Indica que se corrigieron únicamente datos administrativos. */
    @Column(nullable = false)
    private Boolean editada = false;

    @Column(name = "fecha_ultima_edicion")
    private LocalDateTime fechaUltimaEdicion;

    /** Venta anulada que dio origen a esta venta corregida. */
    @Column(name = "id_venta_origen")
    private Long idVentaOrigen;

    /** Nueva venta que reemplazó a esta venta después de anularla. */
    @Column(name = "id_venta_reemplazo")
    private Long idVentaReemplazo;

    @Column(name = "motivo_anulacion", columnDefinition = "TEXT")
    private String motivoAnulacion;

    @JsonManagedReference
    @OneToMany(mappedBy = "venta", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DetalleVenta> detalles;
}
