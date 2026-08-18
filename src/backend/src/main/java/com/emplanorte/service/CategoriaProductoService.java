package com.emplanorte.service;

import com.emplanorte.model.CategoriaProducto;
import com.emplanorte.repository.CategoriaProductoRepository;
import com.emplanorte.repository.ProductoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class CategoriaProductoService {

    private final CategoriaProductoRepository categoriaRepository;
    private final ProductoRepository productoRepository;

    public CategoriaProductoService(CategoriaProductoRepository categoriaRepository,
                                    ProductoRepository productoRepository) {
        this.categoriaRepository = categoriaRepository;
        this.productoRepository = productoRepository;
    }

    public List<CategoriaProducto> listarActivas() {
        return categoriaRepository.findByActivoTrue();
    }

    @Transactional
    public CategoriaProducto crear(CategoriaProducto datos) {
        String nombre = validarYNormalizarNombre(datos);
        CategoriaProducto categoria = categoriaRepository.findByNombreIgnoreCase(nombre)
                .map(existente -> {
                    if (Boolean.TRUE.equals(existente.getActivo())) {
                        throw new IllegalArgumentException("Ya existe una categoría con ese nombre");
                    }
                    return existente;
                })
                .orElseGet(CategoriaProducto::new);

        categoria.setNombre(nombre);
        categoria.setDescripcion(normalizarDescripcion(datos.getDescripcion()));
        categoria.setActivo(true);
        return categoriaRepository.save(categoria);
    }

    @Transactional
    public CategoriaProducto actualizar(Long id, CategoriaProducto datos) {
        CategoriaProducto categoria = obtener(id);
        String nombre = validarYNormalizarNombre(datos);

        categoriaRepository.findByNombreIgnoreCase(nombre)
                .filter(otra -> !otra.getId().equals(id))
                .ifPresent(otra -> {
                    throw new IllegalArgumentException("Ya existe una categoría con ese nombre");
                });

        categoria.setNombre(nombre);
        categoria.setDescripcion(normalizarDescripcion(datos.getDescripcion()));
        return categoriaRepository.save(categoria);
    }

    @Transactional
    public void eliminar(Long id) {
        CategoriaProducto categoria = obtener(id);
        if (productoRepository.existsByCategoriaIdAndActivoTrue(id)) {
            throw new IllegalArgumentException(
                    "No se puede eliminar la categoría porque tiene productos activos. "
                            + "Reasigne esos productos a otra categoría e intente nuevamente."
            );
        }

        // Eliminación lógica: conserva las relaciones de productos inactivos y los históricos.
        categoria.setActivo(false);
        categoriaRepository.save(categoria);
    }

    private CategoriaProducto obtener(Long id) {
        return categoriaRepository.findById(id)
                .filter(categoria -> Boolean.TRUE.equals(categoria.getActivo()))
                .orElseThrow(() -> new NoSuchElementException("Categoría no encontrada"));
    }

    private String validarYNormalizarNombre(CategoriaProducto datos) {
        if (datos == null || datos.getNombre() == null || datos.getNombre().isBlank()) {
            throw new IllegalArgumentException("El nombre de la categoría es obligatorio");
        }
        return datos.getNombre().trim().replaceAll("\\s+", " ");
    }

    private String normalizarDescripcion(String descripcion) {
        if (descripcion == null || descripcion.isBlank()) {
            return null;
        }
        return descripcion.trim();
    }
}
