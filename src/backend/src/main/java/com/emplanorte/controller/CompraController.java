package com.emplanorte.controller;
import com.emplanorte.dto.*;import com.emplanorte.service.CompraService;import org.springframework.http.*;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/compras")
public class CompraController {private final CompraService s;public CompraController(CompraService s){this.s=s;}
@GetMapping public ResponseEntity<?> listar(){return ResponseEntity.ok(s.listar());}
@GetMapping("/{id}") public ResponseEntity<?> obtener(@PathVariable Long id){return ok(()->s.obtener(id),HttpStatus.OK);}
@GetMapping("/{id}/detalles") public ResponseEntity<?> detalles(@PathVariable Long id){return ok(()->s.detalles(id),HttpStatus.OK);}
@GetMapping("/{id}/auditoria") public ResponseEntity<?> auditoria(@PathVariable Long id){return ResponseEntity.ok(s.auditoria(id));}
@PostMapping public ResponseEntity<?> crear(@RequestBody CompraRequest r){return ok(()->s.registrar(r),HttpStatus.CREATED);}
@PostMapping("/{id}/anular") public ResponseEntity<?> anular(@PathVariable Long id,@RequestBody AnulacionRequest r){return ok(()->s.anular(id,r),HttpStatus.OK);}
private ResponseEntity<?> ok(java.util.concurrent.Callable<?> c,HttpStatus h){try{return ResponseEntity.status(h).body(c.call());}catch(Exception e){return ResponseEntity.badRequest().body(e.getMessage());}}
}
