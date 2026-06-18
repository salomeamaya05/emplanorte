package com.emplanorte.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import com.emplanorte.model.DetalleVenta;

import jakarta.transaction.Transactional;

import java.util.List; // Asegúrate de tener este import

@Repository
public interface DetalleVentaRepository extends JpaRepository<DetalleVenta, Long> {

    @Transactional
    // El método de eliminación que agregamos antes
    void deleteByVentaId(Long idVenta);

    // ¡AGREGA ESTA LÍNEA AQUÍ PARA REPARAR LA CARGA DE PRODUCTOS!
    List<DetalleVenta> findByVentaId(Long idVenta);
}