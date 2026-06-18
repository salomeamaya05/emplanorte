package com.emplanorte.controller;

import com.emplanorte.dto.DashboardResponse;
import com.emplanorte.service.DashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    // RF11 - Resumen diario (cuando desde = hasta = hoy)
    // RF12, RNF07 - Estadísticas filtradas por rango de fechas
    @GetMapping("/resumen")
    public ResponseEntity<DashboardResponse> obtenerResumen(
            @RequestParam(value = "desde", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(value = "hasta", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        
        if (desde == null) {
            desde = LocalDate.now();
        }
        if (hasta == null) {
            hasta = LocalDate.now();
        }
        
        return ResponseEntity.ok(dashboardService.obtenerResumenFinanciero(desde, hasta));
    }
}
