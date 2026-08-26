package com.emplanorte.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "detalle_compras")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DetalleCompra {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_compra", nullable = false)
    private Compra compra;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_producto", nullable = false)
    private Producto producto;

    @Column(nullable = false) private Integer cantidad;
    @Column(name = "cantidad_pacas", nullable = false) private Integer cantidadPacas;
    @Column(name = "unidades_por_paca", nullable = false) private Integer unidadesPorPaca;
    @Column(name = "costo_unitario", nullable = false) private BigDecimal costoUnitario;
    @Column(name = "costo_unitario_inventario", nullable = false) private BigDecimal costoUnitarioInventario;
    @Column(name = "subtotal_linea", nullable = false) private BigDecimal subtotalLinea;
    @Column(name = "flete_asignado", nullable = false) private BigDecimal fleteAsignado;
    @Column(name = "flete_unitario", nullable = false) private BigDecimal fleteUnitario;
    @Column(name = "stock_anterior", nullable = false) private Integer stockAnterior;
    @Column(name = "costo_anterior", nullable = false) private BigDecimal costoAnterior;
    @Column(name = "stock_posterior", nullable = false) private Integer stockPosterior;
    @Column(name = "costo_promedio_posterior", nullable = false) private BigDecimal costoPromedioPosterior;
}
