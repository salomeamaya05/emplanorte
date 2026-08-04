package com.emplanorte.service;

import com.emplanorte.dto.FacturaProveedorRequest;
import com.emplanorte.model.*;
import com.emplanorte.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.time.LocalDate;
import java.util.*;

@Service
public class FacturaProveedorService {
    private final FacturaProveedorRepository facturaRepo;
    private final CompraRepository compraRepo;
    private final SupabaseStorageService storage;
    public FacturaProveedorService(FacturaProveedorRepository f, CompraRepository c, SupabaseStorageService s){facturaRepo=f;compraRepo=c;storage=s;}

    public List<FacturaProveedor> listar(){return facturaRepo.findAllByOrderByFechaVencimientoAscIdDesc();}
    public FacturaProveedor obtener(Long id){return facturaRepo.findById(id).orElseThrow(()->new RuntimeException("Factura no encontrada"));}
    public Optional<FacturaProveedor> porCompra(Long idCompra){return facturaRepo.findByCompraId(idCompra);}

    @Transactional
    public FacturaProveedor crear(FacturaProveedorRequest r){
        if(r==null||r.getIdCompra()==null)throw new RuntimeException("Debe seleccionar una compra");
        Compra c=compraRepo.findById(r.getIdCompra()).orElseThrow(()->new RuntimeException("Compra no encontrada"));
        if("anulada".equals(c.getEstado()))throw new RuntimeException("No se puede facturar una compra anulada");
        if(facturaRepo.existsByCompraId(c.getId()))throw new RuntimeException("Esta compra ya tiene una factura registrada");
        return crearDesdeCompra(c,r.getNumeroFactura(),r.getFechaEmision(),r.getFechaVencimiento(),r.getObservaciones());
    }

    @Transactional
    public FacturaProveedor crearDesdeCompra(Compra c,String numero,LocalDate emision,LocalDate vencimiento,String observaciones){
        if(numero==null||numero.isBlank())throw new RuntimeException("El número de factura es obligatorio");
        String n=numero.trim();
        if(facturaRepo.existsByProveedorIdAndNumeroFacturaIgnoreCase(c.getProveedor().getId(),n))throw new RuntimeException("Ese número de factura ya está registrado para el proveedor");
        FacturaProveedor f=new FacturaProveedor();f.setCompra(c);f.setProveedor(c.getProveedor());f.setNumeroFactura(n);
        f.setFechaEmision(emision!=null?emision:c.getFechaCompra().toLocalDate());f.setFechaVencimiento(vencimiento);
        f.setTotalFactura(c.getTotal());f.setTotalPagado(java.math.BigDecimal.ZERO);f.setSaldoPendiente(c.getTotal());
        f.setEstadoPago(c.getTotal().signum()==0?"pagada":"pendiente");f.setObservaciones(observaciones);
        return facturaRepo.save(f);
    }

    public List<FacturaProveedor> alertas(int dias){return facturaRepo.buscarAlertas(LocalDate.now().plusDays(Math.max(0,dias)));}

    @Transactional
    public FacturaProveedor guardarAdjunto(Long id, MultipartFile archivo){
        FacturaProveedor f=obtener(id);
        if(archivo==null||archivo.isEmpty())throw new RuntimeException("Debe seleccionar un archivo");
        String tipo=archivo.getContentType();
        Set<String> permitidos=Set.of("application/pdf","image/jpeg","image/png","image/webp");
        if(tipo==null||!permitidos.contains(tipo))throw new RuntimeException("Solo se permiten PDF, JPG, PNG o WEBP");
        if(archivo.getSize()>10L*1024*1024)throw new RuntimeException("El archivo supera el máximo de 10 MB");
        try{
            String ext=extension(archivo.getOriginalFilename(),tipo);
            String ruta="proveedor-"+f.getProveedor().getId()+"/factura-"+f.getId()+"-"+System.currentTimeMillis()+ext;
            storage.subir(ruta,archivo.getBytes(),tipo);
            f.setRutaAdjunto(ruta);f.setNombreAdjunto(archivo.getOriginalFilename());f.setTipoAdjunto(tipo);
            return facturaRepo.save(f);
        }catch(java.io.IOException e){throw new RuntimeException("No fue posible leer el archivo adjunto");}
    }

    public SupabaseStorageService.ArchivoDescargado descargarAdjunto(Long id){
        FacturaProveedor f=obtener(id);if(f.getRutaAdjunto()==null)throw new RuntimeException("La factura no tiene archivo adjunto");
        return storage.descargar(f.getRutaAdjunto(),f.getTipoAdjunto(),f.getNombreAdjunto());
    }

    private String extension(String n,String t){
        if(n!=null&&n.lastIndexOf('.')>=0)return n.substring(n.lastIndexOf('.')).toLowerCase();
        return switch(t){case "application/pdf"->".pdf";case "image/png"->".png";case "image/webp"->".webp";default->".jpg";};
    }
}
