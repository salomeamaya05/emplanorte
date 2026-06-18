package com.emplanorte.controller;

import com.emplanorte.model.CategoriaProducto;
import com.emplanorte.repository.CategoriaProductoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categorias-producto")
public class CategoriaProductoController {

    @Autowired
    private CategoriaProductoRepository categoriaProductoRepository;

    @GetMapping
    public ResponseEntity<List<CategoriaProducto>> listarCategorias() {
        return ResponseEntity.ok(categoriaProductoRepository.findAll());
    }

    @PostMapping
    public ResponseEntity<CategoriaProducto> crearCategoria(@RequestBody CategoriaProducto categoria) {
        categoria.setActivo(true);
        return ResponseEntity.ok(categoriaProductoRepository.save(categoria));
    }
}
