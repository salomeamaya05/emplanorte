package com.emplanorte.repository;

import com.emplanorte.model.AbonoCredito;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AbonoCreditoRepository extends JpaRepository<AbonoCredito, Long> {

    @EntityGraph(attributePaths = "usuario")
    List<AbonoCredito> findByCreditoIdOrderByFechaPagoAscIdAsc(Long idCredito);

    @EntityGraph(attributePaths = "usuario")
    Optional<AbonoCredito> findByClaveIdempotencia(String claveIdempotencia);
}
