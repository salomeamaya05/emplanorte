package com.emplanorte.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.emplanorte.dto.VentaRequest;
import com.emplanorte.model.AuditoriaVenta;
import com.emplanorte.model.Venta;
import com.emplanorte.service.VentaService;

@RestController
@RequestMapping("/api/ventas")
public class VentaController {

    @Autowired
    private VentaService ventaService;

    @GetMapping
    public ResponseEntity<List<Venta>> listarVentas() {
        return ResponseEntity.ok(ventaService.obtenerTodas());
    }

    // RF05, RF06, RF07, RF08 - Registrar Venta
    @PostMapping
    public ResponseEntity<?> registrarVenta(@RequestBody VentaRequest request) {
        try {
            Venta venta = ventaService.registrarVenta(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(venta);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }
    @GetMapping("/{id}/detalles")
    public ResponseEntity<?> listarDetallesVenta(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(ventaService.obtenerDetalles(id));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // Anular venta (inmutable): requiere contraseña del usuario; devuelve el stock
    @PostMapping("/{id}/anular")
    public ResponseEntity<?> anularVenta(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            Long idUsuario = body.get("idUsuario") != null
                    ? Long.valueOf(body.get("idUsuario").toString()) : null;
            String contrasena = body.get("contrasena") != null ? body.get("contrasena").toString() : null;
            Venta venta = ventaService.anularVenta(id, idUsuario, contrasena);
            return ResponseEntity.ok(venta);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    // Bitácora de auditoría de una venta
    @GetMapping("/{id}/auditoria")
    public ResponseEntity<List<AuditoriaVenta>> obtenerAuditoriaVenta(@PathVariable Long id) {
        return ResponseEntity.ok(ventaService.obtenerAuditoria(id));
    }

}
