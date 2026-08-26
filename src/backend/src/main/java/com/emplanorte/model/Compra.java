package com.emplanorte.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "compras")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Compra {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_compra", nullable = false, unique = true, length = 30)
    private String numeroCompra;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_proveedor", nullable = false)
    private Proveedor proveedor;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "id_usuario", nullable = false)
    private Usuario usuario;

    @Column(name = "fecha_compra", nullable = false)
    private LocalDateTime fechaCompra;

    @Column(nullable = false) private BigDecimal subtotal;
    @Column(nullable = false) private BigDecimal flete;
    @Column(nullable = false) private BigDecimal impuestos;
    @Column(nullable = false) private BigDecimal descuento;
    @Column(nullable = false) private BigDecimal total;

    @Column(name = "metodo_distribucion_flete", nullable = false, length = 20)
    private String metodoDistribucionFlete = "pacas";

    @Column(nullable = false, length = 20)
    private String estado = "registrada";

    @Column(columnDefinition = "TEXT") private String observaciones;
    @Column(name = "motivo_anulacion", columnDefinition = "TEXT") private String motivoAnulacion;
    @Column(name = "creado_en", nullable = false) private LocalDateTime creadoEn;
    @Column(name = "anulado_en") private LocalDateTime anuladoEn;

    @JsonIgnore
    @OneToMany(mappedBy = "compra", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<DetalleCompra> detalles = new ArrayList<>();

    @PrePersist
    void prePersist() {
        LocalDateTime ahora = LocalDateTime.now();
        if (fechaCompra == null) fechaCompra = ahora;
        if (creadoEn == null) creadoEn = ahora;
        if (estado == null) estado = "registrada";
        if (metodoDistribucionFlete == null) metodoDistribucionFlete = "pacas";
    }
}
