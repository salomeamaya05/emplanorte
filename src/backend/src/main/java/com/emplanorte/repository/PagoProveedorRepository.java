package com.emplanorte.repository;
import com.emplanorte.model.PagoProveedor;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface PagoProveedorRepository extends JpaRepository<PagoProveedor, Long> {
    List<PagoProveedor> findByFacturaIdOrderByFechaPagoDescIdDesc(Long idFactura);
    boolean existsByFacturaIdAndEstado(Long idFactura, String estado);
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PagoProveedor p WHERE p.id=:id")
    Optional<PagoProveedor> buscarPorIdParaActualizar(@Param("id") Long id);
}
