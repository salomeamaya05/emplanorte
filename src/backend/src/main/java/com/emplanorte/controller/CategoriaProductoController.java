package com.emplanorte.controller;

import com.emplanorte.model.CategoriaProducto;
import com.emplanorte.service.CategoriaProductoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/categorias-producto")
public class CategoriaProductoController {

    private final CategoriaProductoService categoriaService;

    public CategoriaProductoController(CategoriaProductoService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping
    public ResponseEntity<List<CategoriaProducto>> listarCategorias() {
        return ResponseEntity.ok(categoriaService.listarActivas());
    }

    @PostMapping
    public ResponseEntity<?> crearCategoria(@RequestBody CategoriaProducto categoria) {
        try {
            return ResponseEntity.ok(categoriaService.crear(categoria));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarCategoria(@PathVariable Long id,
                                                 @RequestBody CategoriaProducto categoria) {
        try {
            return ResponseEntity.ok(categoriaService.actualizar(id, categoria));
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarCategoria(@PathVariable Long id) {
        try {
            categoriaService.eliminar(id);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(404).body(Map.of("message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }
}
