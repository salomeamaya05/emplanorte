package com.emplanorte.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.emplanorte.model.AuditoriaGasto;
import com.emplanorte.model.CategoriaGasto;
import com.emplanorte.model.Gasto;
import com.emplanorte.model.Usuario;
import com.emplanorte.repository.AuditoriaGastoRepository;
import com.emplanorte.repository.CategoriaGastoRepository;
import com.emplanorte.repository.GastoRepository;
import com.emplanorte.repository.UsuarioRepository;

@Service
public class GastoService {

    @Autowired
    private GastoRepository gastoRepository;

    @Autowired
    private CategoriaGastoRepository categoriaGastoRepository;

    @Autowired
    private AuditoriaGastoRepository auditoriaGastoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    // Métodos de Gastos (RF09)
    public List<Gasto> obtenerTodos() {
        return gastoRepository.findAll();
    }

    public List<Gasto> obtenerPorFechas(LocalDate desde, LocalDate hasta) {
        return gastoRepository.findByFechaGastoBetween(desde, hasta);
    }

    @Transactional
    public Gasto guardar(Gasto gasto) {
        // CP-29 - El valor del gasto debe ser positivo
        if (gasto.getValor() == null || gasto.getValor().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("El valor del gasto debe ser mayor a cero");
        }
        // CP-28 - No se permiten gastos con fecha futura
        if (gasto.getFechaGasto() != null && gasto.getFechaGasto().isAfter(LocalDate.now())) {
            throw new RuntimeException("La fecha del gasto no puede ser futura");
        }
        if (gasto.getFechaGasto() == null) {
            gasto.setFechaGasto(LocalDate.now());
        }
        Gasto guardado = gastoRepository.save(gasto);
        registrarAuditoria(guardado, "creacion", gasto.getUsuario());
        return guardado;
    }

    @Transactional
    public Gasto actualizarGasto(Long id, Gasto gastoActualizado) {
        Gasto gasto = gastoRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Gasto no encontrado"));

        gasto.setCategoria(gastoActualizado.getCategoria());
        gasto.setDescripcion(gastoActualizado.getDescripcion());
        gasto.setValor(gastoActualizado.getValor());
        gasto.setFechaGasto(gastoActualizado.getFechaGasto());

        Gasto guardado = gastoRepository.save(gasto);
        // El "actor" de la edición viene del usuario enviado en la petición.
        registrarAuditoria(guardado, "edicion", gastoActualizado.getUsuario());
        return guardado;
    }

    // Historial de cambios de un gasto (bitácora de auditoría)
    public List<AuditoriaGasto> obtenerAuditoria(Long idGasto) {
        return auditoriaGastoRepository.findByIdGastoOrderByFechaRegistroAsc(idGasto);
    }

    // Guarda un snapshot inmutable del gasto tras una creación/edición.
    private void registrarAuditoria(Gasto gasto, String accion, Usuario actor) {
        AuditoriaGasto a = new AuditoriaGasto();
        a.setIdGasto(gasto.getId());
        a.setAccion(accion);
        a.setDescripcion(gasto.getDescripcion());
        a.setValor(gasto.getValor());
        a.setFechaGasto(gasto.getFechaGasto());

        if (gasto.getCategoria() != null && gasto.getCategoria().getId() != null) {
            categoriaGastoRepository.findById(gasto.getCategoria().getId())
                    .ifPresent(c -> a.setCategoriaNombre(c.getNombre()));
        }
        if (actor != null && actor.getId() != null) {
            a.setIdUsuario(actor.getId());
            usuarioRepository.findById(actor.getId())
                    .ifPresent(u -> a.setUsuarioNombre(u.getNombre()));
        }
        auditoriaGastoRepository.save(a);
    }

    // Métodos de Categorías de Gasto (RF10)
    public List<CategoriaGasto> obtenerCategoriasActivas() {
        return categoriaGastoRepository.findByActivoTrue();
    }

    public CategoriaGasto guardarCategoria(CategoriaGasto categoria) {
        if (categoria.getNombre() == null || categoria.getNombre().isBlank()) {
            throw new RuntimeException("El nombre de la categoría es obligatorio");
        }
        // CP-31 - No permitir categorías de gasto duplicadas
        boolean duplicada = categoriaGastoRepository.findByActivoTrue().stream()
                .anyMatch(c -> c.getNombre() != null
                        && c.getNombre().trim().equalsIgnoreCase(categoria.getNombre().trim()));
        if (duplicada) {
            throw new RuntimeException("La categoría ya existe: " + categoria.getNombre().trim());
        }
        categoria.setActivo(true);
        return categoriaGastoRepository.save(categoria);
    }
}
