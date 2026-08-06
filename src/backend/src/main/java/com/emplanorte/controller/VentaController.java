package com.emplanorte.controller;

import com.emplanorte.dto.AnulacionRequest;
import com.emplanorte.dto.VentaEdicionRequest;
import com.emplanorte.dto.VentaRequest;
import com.emplanorte.model.AuditoriaVenta;
import com.emplanorte.model.Venta;
import com.emplanorte.service.VentaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ventas")
public class VentaController {

    @Autowired
    private VentaService ventaService;

    @GetMapping
    public ResponseEntity<List<Venta>> listarVentas() {
        return ResponseEntity.ok(ventaService.obtenerTodas());
    }

    @PostMapping
    public ResponseEntity<?> registrarVenta(@RequestBody VentaRequest request) {
        try {
            Venta venta = ventaService.registrarVenta(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(venta);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> editarVenta(
            @PathVariable Long id,
            @RequestBody VentaEdicionRequest request
    ) {
        try {
            return ResponseEntity.ok(ventaService.editarDatosAdministrativos(id, request));
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

    @PostMapping("/{id}/anular")
    public ResponseEntity<?> anularVenta(
            @PathVariable Long id,
            @RequestBody AnulacionRequest request
    ) {
        try {
            Venta venta = ventaService.anularVenta(
                    id,
                    request.getIdUsuario(),
                    request.getContrasena(),
                    request.getMotivo(),
                    Boolean.TRUE.equals(request.getCorregir())
            );
            return ResponseEntity.ok(venta);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        }
    }

    @GetMapping("/{id}/auditoria")
    public ResponseEntity<List<AuditoriaVenta>> obtenerAuditoriaVenta(@PathVariable Long id) {
        return ResponseEntity.ok(ventaService.obtenerAuditoria(id));
    }
}
