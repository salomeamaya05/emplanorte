package com.emplanorte.controller;
import com.emplanorte.dto.*;import com.emplanorte.service.PagoProveedorService;import org.springframework.http.*;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/facturas-proveedores")
public class PagoProveedorController {private final PagoProveedorService s;public PagoProveedorController(PagoProveedorService s){this.s=s;}
@GetMapping("/{idFactura}/pagos") public ResponseEntity<?> listar(@PathVariable Long idFactura){return ResponseEntity.ok(s.listar(idFactura));}
@PostMapping("/{idFactura}/pagos") public ResponseEntity<?> crear(@PathVariable Long idFactura,@RequestBody PagoProveedorRequest r){return ejecutar(()->s.registrar(idFactura,r),HttpStatus.CREATED);}
@PostMapping("/pagos/{idPago}/anular") public ResponseEntity<?> anular(@PathVariable Long idPago,@RequestBody AnulacionRequest r){return ejecutar(()->s.anular(idPago,r),HttpStatus.OK);}
private ResponseEntity<?> ejecutar(java.util.concurrent.Callable<?> c,HttpStatus h){try{return ResponseEntity.status(h).body(c.call());}catch(Exception e){return ResponseEntity.badRequest().body(e.getMessage());}}
}
