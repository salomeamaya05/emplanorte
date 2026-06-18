package com.emplanorte.controller;

import com.emplanorte.dto.CotizacionRequest;
import com.emplanorte.model.Cotizacion;
import com.emplanorte.model.Venta;
import com.emplanorte.service.CotizacionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/cotizaciones")
public class CotizacionController {

    @Autowired
    private CotizacionService cotizacionService;

    @GetMapping
    public ResponseEntity<List<Cotizacion>> listarCotizaciones() {
        return ResponseEntity.ok(cotizacionService.obtenerTodas());
    }
    @GetMapping("/{id}/detalles")
public ResponseEntity<?> listarDetallesCotizacion(@PathVariable Long id) {
    try {
        return ResponseEntity.ok(cotizacionService.obtenerDetalles(id));
    } catch (RuntimeException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }
}

@PutMapping("/{id}")
public ResponseEntity<?> actualizarCotizacion(@PathVariable Long id, @RequestBody CotizacionRequest request) {
    try {
        Cotizacion cotizacion = cotizacionService.actualizarCotizacion(id, request);
        return ResponseEntity.ok(cotizacion);
    } catch (RuntimeException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
    }
}

    // RF14 - Crear cotización
    @PostMapping
    public ResponseEntity<?> crearCotizacion(@RequestBody CotizacionRequest request) {
        try {
            Cotizacion cotizacion = cotizacionService.registrarCotizacion(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(cotizacion);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // RF14 - Eliminar cotización (solo pendiente/rechazada)
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminarCotizacion(@PathVariable Long id) {
        try {
            cotizacionService.eliminarCotizacion(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // RF14, RF05 - Convertir cotización en venta real
    @PostMapping("/convertir/{id}")
    public ResponseEntity<?> convertirAVenta(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String metodoPago = body.get("metodoPago");
        if (metodoPago == null) {
            metodoPago = "efectivo"; // Por defecto si no se especifica
        }
        String referenciaPago = body.get("referenciaPago"); // null si no viene de la pasarela
        try {
            Venta venta = cotizacionService.convertirAVenta(id, metodoPago, referenciaPago);
            return ResponseEntity.ok(venta);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
}
