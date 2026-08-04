package com.emplanorte.repository;
import com.emplanorte.model.DetalleCompra;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface DetalleCompraRepository extends JpaRepository<DetalleCompra, Long> {
    List<DetalleCompra> findByCompraIdOrderByIdAsc(Long idCompra);
}
