package com.emplanorte.repository;

import com.emplanorte.model.CreditoVenta;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CreditoVentaRepository extends JpaRepository<CreditoVenta, Long> {

    @EntityGraph(attributePaths = {"venta", "cliente"})
    List<CreditoVenta> findAllByOrderByFechaVencimientoAscIdDesc();

    @EntityGraph(attributePaths = {"venta", "cliente"})
    Optional<CreditoVenta> findByVentaId(Long idVenta);

    @EntityGraph(attributePaths = {"venta", "cliente"})
    Optional<CreditoVenta> findOneById(Long id);

    @EntityGraph(attributePaths = {"venta", "cliente"})
    List<CreditoVenta> findByClienteIdOrderByFechaVencimientoDesc(Long idCliente);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @EntityGraph(attributePaths = {"venta", "cliente"})
    @Query("SELECT c FROM CreditoVenta c WHERE c.id = :id")
    Optional<CreditoVenta> buscarPorIdParaActualizar(@Param("id") Long id);
}
