package com.emplanorte.repository;

import com.emplanorte.model.AuditoriaVenta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditoriaVentaRepository extends JpaRepository<AuditoriaVenta, Long> {

    List<AuditoriaVenta> findByIdVentaOrderByFechaRegistroAsc(Long idVenta);
}
