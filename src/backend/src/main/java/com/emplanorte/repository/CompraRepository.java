package com.emplanorte.repository;
import com.emplanorte.model.Compra;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
public interface CompraRepository extends JpaRepository<Compra, Long> {
    List<Compra> findAllByOrderByFechaCompraDescIdDesc();
    List<Compra> findByProveedorIdOrderByFechaCompraDesc(Long idProveedor);
    @Query(value="SELECT fn_generar_numero_compra()", nativeQuery=true)
    String generarNumeroCompra();
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM Compra c WHERE c.id=:id")
    Optional<Compra> buscarPorIdParaActualizar(@Param("id") Long id);
    @Query("SELECT COALESCE(SUM(c.total),0) FROM Compra c WHERE c.fechaCompra BETWEEN :desde AND :hasta AND c.estado='registrada'")
    BigDecimal totalPorRango(@Param("desde") LocalDateTime desde,@Param("hasta") LocalDateTime hasta);
}
