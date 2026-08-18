package com.emplanorte.controller;
import com.emplanorte.dto.ProveedorRequest;import com.emplanorte.service.ProveedorService;import org.springframework.http.*;import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/proveedores")
public class ProveedorController {private final ProveedorService s;public ProveedorController(ProveedorService s){this.s=s;}
@GetMapping public ResponseEntity<?> listar(@RequestParam(defaultValue="false") boolean incluirInactivos){return ResponseEntity.ok(s.listar(incluirInactivos));}
@GetMapping("/{id}") public ResponseEntity<?> obtener(@PathVariable Long id){return ejecutar(()->s.obtener(id),HttpStatus.OK);}
@GetMapping("/{id}/resumen") public ResponseEntity<?> resumen(@PathVariable Long id){return ejecutar(()->s.resumen(id),HttpStatus.OK);}
@PostMapping public ResponseEntity<?> crear(@RequestBody ProveedorRequest r){return ejecutar(()->s.crear(r),HttpStatus.CREATED);}
@PutMapping("/{id}") public ResponseEntity<?> actualizar(@PathVariable Long id,@RequestBody ProveedorRequest r){return ejecutar(()->s.actualizar(id,r),HttpStatus.OK);}
@DeleteMapping("/{id}") public ResponseEntity<?> desactivar(@PathVariable Long id){try{s.desactivar(id);return ResponseEntity.noContent().build();}catch(RuntimeException e){return ResponseEntity.badRequest().body(e.getMessage());}}
private ResponseEntity<?> ejecutar(java.util.concurrent.Callable<?> c,HttpStatus h){try{return ResponseEntity.status(h).body(c.call());}catch(Exception e){return ResponseEntity.badRequest().body(e.getMessage());}}
}
