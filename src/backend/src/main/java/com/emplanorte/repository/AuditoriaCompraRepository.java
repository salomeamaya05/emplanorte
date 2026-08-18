package com.emplanorte.repository;
import com.emplanorte.model.AuditoriaCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface AuditoriaCompraRepository extends JpaRepository<AuditoriaCompra, Long> {
    List<AuditoriaCompra> findByIdCompraOrderByFechaRegistroAsc(Long idCompra);
}
