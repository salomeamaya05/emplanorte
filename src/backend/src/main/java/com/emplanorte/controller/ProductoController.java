package com.emplanorte.controller;

import com.emplanorte.model.Producto;
import com.emplanorte.dto.FusionProductoRequest;
import com.emplanorte.service.ProductoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/productos")
public class ProductoController {

    @Autowired
    private ProductoService productoService;

    // Obtener todos los productos activos, opcionalmente ordenados (RF04)
    @GetMapping
    public ResponseEntity<List<Producto>> listarProductos(@RequestParam(value = "ordenarPor", required = false) String ordenarPor) {
        if (ordenarPor != null) {
            return ResponseEntity.ok(productoService.obtenerTodosActivosOrdenados(ordenarPor));
        }
        return ResponseEntity.ok(productoService.obtenerTodosActivos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Producto> obtenerProducto(@PathVariable Long id) {
        return productoService.obtenerPorId(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // RF01, RF03 - Registro de productos
    @PostMapping
    public ResponseEntity<?> crearProducto(@RequestBody Producto producto) {
        try {
            return ResponseEntity.ok(productoService.guardar(producto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // RF02 - Modificación de productos
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizarProducto(@PathVariable Long id, @RequestBody Producto producto) {
        try {
            return ResponseEntity.ok(productoService.actualizar(id, producto));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

    // RF02 - Eliminación lógica de productos
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarProducto(@PathVariable Long id) {
        try {
            productoService.desactivar(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }


    @PostMapping("/fusionar")
    public ResponseEntity<?> fusionarProductos(@RequestBody FusionProductoRequest request) {
        try {
            return ResponseEntity.ok(productoService.fusionar(
                    request.getProductoDestinoId(),
                    request.getProductoDuplicadoId()
            ));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(Map.of("message", e.getMessage()));
        }
    }

}
