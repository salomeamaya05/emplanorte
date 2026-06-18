package com.emplanorte.repository;

import com.emplanorte.model.Cotizacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CotizacionRepository extends JpaRepository<Cotizacion, Long> {
    List<Cotizacion> findByEstado(String estado);

    @Query(value = "SELECT fn_generar_numero_cotizacion()", nativeQuery = true)
    String generarNumeroCotizacion();
}

