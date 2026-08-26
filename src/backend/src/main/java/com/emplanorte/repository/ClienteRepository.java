package com.emplanorte.repository;

import com.emplanorte.model.Cliente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ClienteRepository extends JpaRepository<Cliente, Long> {
    List<Cliente> findByActivoTrue();
    Optional<Cliente> findFirstByNombreIgnoreCase(String nombre);
    Optional<Cliente> findByDocumentoIgnoreCase(String documento);
}
