package com.emplanorte.controller;

import com.emplanorte.model.AuditoriaGasto;
import com.emplanorte.model.CategoriaGasto;
import com.emplanorte.model.Gasto;
import com.emplanorte.service.GastoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/gastos")
public class GastoController {

    @Autowired
    private GastoService gastoService;

    @GetMapping
    public ResponseEntity<List<Gasto>> listarGastos() {
        return ResponseEntity.ok(gastoService.obtenerTodos());
    }

    // RF09, RNF07 - Consulta de información por rango de fechas
    @GetMapping("/rango")
    public ResponseEntity<List<Gasto>> listarGastosPorRango(
            @RequestParam("desde") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam("hasta") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return ResponseEntity.ok(gastoService.obtenerPorFechas(desde, hasta));
    }

    @PostMapping
    public ResponseEntity<Gasto> registrarGasto(@RequestBody Gasto gasto) {
        return ResponseEntity.ok(gastoService.guardar(gasto));
    }
    @PutMapping("/{id}")
public ResponseEntity<Gasto> actualizarGasto(@PathVariable Long id, @RequestBody Gasto gasto) {
    return ResponseEntity.ok(gastoService.actualizarGasto(id, gasto));
}

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminarGasto(@PathVariable Long id) {
        gastoService.eliminarGasto(id);
        return ResponseEntity.noContent().build();
    }

    // Bitácora de auditoría: historial de cambios de un gasto
    @GetMapping("/{id}/auditoria")
    public ResponseEntity<List<AuditoriaGasto>> obtenerAuditoriaGasto(@PathVariable Long id) {
        return ResponseEntity.ok(gastoService.obtenerAuditoria(id));
    }

    // RF10 - Obtener categorías de gastos
    @GetMapping("/categorias")
    public ResponseEntity<List<CategoriaGasto>> listarCategoriasGasto() {
        return ResponseEntity.ok(gastoService.obtenerCategoriasActivas());
    }

    // RF10 - Registrar categoría de gasto
    @PostMapping("/categorias")
    public ResponseEntity<CategoriaGasto> crearCategoriaGasto(@RequestBody CategoriaGasto categoria) {
        return ResponseEntity.ok(gastoService.guardarCategoria(categoria));
    }
}
