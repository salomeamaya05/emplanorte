package com.emplanorte.repository;
import com.emplanorte.model.Proveedor;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.*;
public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {
    List<Proveedor> findAllByOrderByRazonSocialAsc();
    List<Proveedor> findByActivoTrueOrderByRazonSocialAsc();
    Optional<Proveedor> findByNitDocumentoIgnoreCase(String nitDocumento);
}
