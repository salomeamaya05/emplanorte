package com.emplanorte.repository;

import com.emplanorte.model.AuditoriaGasto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditoriaGastoRepository extends JpaRepository<AuditoriaGasto, Long> {

    // Historial de un gasto en orden cronológico (de creación a últimas ediciones)
    List<AuditoriaGasto> findByIdGastoOrderByFechaRegistroAsc(Long idGasto);
}
