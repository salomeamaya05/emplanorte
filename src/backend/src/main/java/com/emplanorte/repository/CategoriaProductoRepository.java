package com.emplanorte.repository;

import com.emplanorte.model.CategoriaProducto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CategoriaProductoRepository extends JpaRepository<CategoriaProducto, Long> {
    List<CategoriaProducto> findByActivoTrue();
    Optional<CategoriaProducto> findByNombreIgnoreCase(String nombre);
}
