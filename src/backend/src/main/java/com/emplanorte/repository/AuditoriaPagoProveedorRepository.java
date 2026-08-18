package com.emplanorte.repository;
import com.emplanorte.model.AuditoriaPagoProveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface AuditoriaPagoProveedorRepository extends JpaRepository<AuditoriaPagoProveedor, Long> {
    List<AuditoriaPagoProveedor> findByIdPagoOrderByFechaRegistroAsc(Long idPago);
}
