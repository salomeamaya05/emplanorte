package com.emplanorte.repository;

import com.emplanorte.model.Gasto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface GastoRepository extends JpaRepository<Gasto, Long> {
    List<Gasto> findByFechaGastoBetween(LocalDate desde, LocalDate hasta);

    @Query("SELECT COALESCE(SUM(g.valor), 0) FROM Gasto g WHERE g.fechaGasto >= :start AND g.fechaGasto <= :end")
    BigDecimal obtenerTotalGastosPorRango(@Param("start") LocalDate start, @Param("end") LocalDate end);
}

