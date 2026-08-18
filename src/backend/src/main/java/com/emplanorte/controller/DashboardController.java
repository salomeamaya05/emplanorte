package com.emplanorte.controller;

import com.emplanorte.dto.*;
import com.emplanorte.service.DashboardService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;
    public DashboardController(DashboardService dashboardService){this.dashboardService=dashboardService;}

    @GetMapping("/resumen")
    public ResponseEntity<?> obtenerResumen(
      @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate desde,
      @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate hasta){
        try{return ResponseEntity.ok(dashboardService.obtenerResumenFinanciero(desde,hasta));}
        catch(RuntimeException e){return ResponseEntity.badRequest().body(e.getMessage());}
    }

    @GetMapping("/balance-completo")
    public ResponseEntity<?> balanceCompleto(
      @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate desde,
      @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate hasta){
        try{return ResponseEntity.ok(dashboardService.obtenerBalanceCompleto(desde,hasta));}
        catch(RuntimeException e){return ResponseEntity.badRequest().body(e.getMessage());}
    }
}
