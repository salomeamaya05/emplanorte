package com.emplanorte.repository;

import com.emplanorte.model.CategoriaGasto;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CategoriaGastoRepository extends JpaRepository<CategoriaGasto, Long> {
    List<CategoriaGasto> findByActivoTrue();
    Optional<CategoriaGasto> findByNombreIgnoreCase(String nombre);
}
