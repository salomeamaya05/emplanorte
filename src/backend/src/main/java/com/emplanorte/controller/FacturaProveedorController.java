package com.emplanorte.controller;
import com.emplanorte.dto.FacturaProveedorRequest;import com.emplanorte.service.*;import org.springframework.http.*;import org.springframework.web.bind.annotation.*;import org.springframework.web.multipart.MultipartFile;
@RestController @RequestMapping("/api/facturas-proveedores")
public class FacturaProveedorController {private final FacturaProveedorService s;public FacturaProveedorController(FacturaProveedorService s){this.s=s;}
@GetMapping public ResponseEntity<?> listar(){return ResponseEntity.ok(s.listar());}
@GetMapping("/{id}") public ResponseEntity<?> obtener(@PathVariable Long id){return ejecutar(()->s.obtener(id),HttpStatus.OK);}
@GetMapping("/compra/{idCompra}") public ResponseEntity<?> porCompra(@PathVariable Long idCompra){return s.porCompra(idCompra).<ResponseEntity<?>>map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());}
@GetMapping("/alertas") public ResponseEntity<?> alertas(@RequestParam(defaultValue="7") int dias){return ResponseEntity.ok(s.alertas(dias));}
@PostMapping public ResponseEntity<?> crear(@RequestBody FacturaProveedorRequest r){return ejecutar(()->s.crear(r),HttpStatus.CREATED);}
@PostMapping(value="/{id}/adjunto",consumes=MediaType.MULTIPART_FORM_DATA_VALUE) public ResponseEntity<?> adjunto(@PathVariable Long id,@RequestPart("archivo") MultipartFile a){return ejecutar(()->s.guardarAdjunto(id,a),HttpStatus.OK);}
@GetMapping("/{id}/adjunto") public ResponseEntity<?> descargar(@PathVariable Long id){try{var a=s.descargarAdjunto(id);return ResponseEntity.ok().contentType(MediaType.parseMediaType(a.tipo())).header(HttpHeaders.CONTENT_DISPOSITION,"inline; filename=\""+a.nombre().replace("\"","")+"\"").body(a.contenido());}catch(RuntimeException e){return ResponseEntity.badRequest().body(e.getMessage());}}
private ResponseEntity<?> ejecutar(java.util.concurrent.Callable<?> c,HttpStatus h){try{return ResponseEntity.status(h).body(c.call());}catch(Exception e){return ResponseEntity.badRequest().body(e.getMessage());}}
}
