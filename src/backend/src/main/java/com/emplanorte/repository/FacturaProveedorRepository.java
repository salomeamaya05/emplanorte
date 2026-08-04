package com.emplanorte.repository;
import com.emplanorte.model.FacturaProveedor;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.*;
public interface FacturaProveedorRepository extends JpaRepository<FacturaProveedor, Long> {
    List<FacturaProveedor> findAllByOrderByFechaVencimientoAscIdDesc();
    Optional<FacturaProveedor> findByCompraId(Long idCompra);
    boolean existsByCompraId(Long idCompra);
    boolean existsByProveedorIdAndNumeroFacturaIgnoreCase(Long idProveedor, String numeroFactura);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT f FROM FacturaProveedor f WHERE f.id=:id")
    Optional<FacturaProveedor> buscarPorIdParaActualizar(@Param("id") Long id);
    @Query("SELECT f FROM FacturaProveedor f WHERE f.estadoPago IN ('pendiente','parcial') AND f.fechaVencimiento IS NOT NULL AND f.fechaVencimiento <= :limite ORDER BY f.fechaVencimiento ASC")
    List<FacturaProveedor> buscarAlertas(@Param("limite") LocalDate limite);
}
