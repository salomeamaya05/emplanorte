package com.emplanorte.controller;

import com.emplanorte.dto.AbonoCreditoRequest;
import com.emplanorte.dto.CreditoActualizacionRequest;
import com.emplanorte.service.CarteraService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/cartera")
public class CarteraController {
    private final CarteraService carteraService;

    public CarteraController(CarteraService carteraService) {
        this.carteraService = carteraService;
    }

    @GetMapping
    public ResponseEntity<?> listar(
            @RequestParam(required = false) String estado,
            @RequestParam(required = false) String buscar
    ) {
        return ResponseEntity.ok(carteraService.listar(estado, buscar));
    }

    @GetMapping("/resumen")
    public ResponseEntity<?> resumen() {
        return ResponseEntity.ok(carteraService.resumen());
    }

    @GetMapping("/venta/{idVenta}")
    public ResponseEntity<?> porVenta(@PathVariable Long idVenta) {
        return ejecutar(() -> carteraService.obtenerPorVenta(idVenta), HttpStatus.OK);
    }

    @GetMapping("/cliente/{idCliente}/resumen")
    public ResponseEntity<?> resumenCliente(@PathVariable Long idCliente) {
        return ejecutar(() -> carteraService.resumenCliente(idCliente), HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> obtener(@PathVariable Long id) {
        return ejecutar(() -> carteraService.obtener(id), HttpStatus.OK);
    }

    @GetMapping("/{id}/abonos")
    public ResponseEntity<?> abonos(@PathVariable Long id) {
        return ejecutar(() -> carteraService.listarAbonos(id), HttpStatus.OK);
    }

    @PostMapping("/{id}/abonos")
    public ResponseEntity<?> registrarAbono(
            @PathVariable Long id,
            @RequestBody AbonoCreditoRequest request
    ) {
        return ejecutar(() -> carteraService.registrarAbono(id, request), HttpStatus.CREATED);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(
            @PathVariable Long id,
            @RequestBody CreditoActualizacionRequest request
    ) {
        return ejecutar(() -> carteraService.actualizar(id, request), HttpStatus.OK);
    }

    private ResponseEntity<?> ejecutar(java.util.concurrent.Callable<?> accion, HttpStatus status) {
        try {
            return ResponseEntity.status(status).body(accion.call());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
