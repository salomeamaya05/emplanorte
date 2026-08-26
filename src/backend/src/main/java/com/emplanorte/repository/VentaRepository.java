package com.emplanorte.repository;

import com.emplanorte.model.Venta;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface VentaRepository extends JpaRepository<Venta, Long> {
    List<Venta> findByEstado(String estado);
    List<Venta> findByClienteIdAndEstado(Long idCliente, String estado);

    @Query(value = "SELECT fn_generar_numero_venta()", nativeQuery = true)
    String generarNumeroVenta();

    @Query("SELECT COALESCE(SUM(v.total), 0) FROM Venta v WHERE v.fechaVenta >= :start AND v.fechaVenta <= :end AND v.estado = :estado")
    BigDecimal obtenerTotalVentasPorRango(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("estado") String estado);

    @Query("SELECT COALESCE(SUM(v.ganancia), 0) FROM Venta v WHERE v.fechaVenta >= :start AND v.fechaVenta <= :end AND v.estado = :estado")
    BigDecimal obtenerGananciaVentasPorRango(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end, @Param("estado") String estado);
}

