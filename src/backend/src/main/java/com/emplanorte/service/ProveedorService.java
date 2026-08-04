package com.emplanorte.service;

import com.emplanorte.dto.ProveedorRequest;
import com.emplanorte.model.Proveedor;
import com.emplanorte.repository.ProveedorRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.util.*;

@Service
public class ProveedorService {
    private final ProveedorRepository repository;
    private final JdbcTemplate jdbc;
    public ProveedorService(ProveedorRepository repository, JdbcTemplate jdbc){this.repository=repository;this.jdbc=jdbc;}

    public List<Proveedor> listar(boolean incluirInactivos){
        return incluirInactivos ? repository.findAllByOrderByRazonSocialAsc() : repository.findByActivoTrueOrderByRazonSocialAsc();
    }
    public Proveedor obtener(Long id){return repository.findById(id).orElseThrow(()->new RuntimeException("Proveedor no encontrado"));}

    @Transactional
    public Proveedor crear(ProveedorRequest r){
        validar(r);
        repository.findByNitDocumentoIgnoreCase(r.getNitDocumento().trim()).ifPresent(x->{throw new RuntimeException("Ya existe un proveedor con ese NIT o documento");});
        Proveedor p=new Proveedor(); aplicar(p,r); p.setActivo(true); return repository.save(p);
    }

    @Transactional
    public Proveedor actualizar(Long id, ProveedorRequest r){
        validar(r); Proveedor p=obtener(id);
        repository.findByNitDocumentoIgnoreCase(r.getNitDocumento().trim()).ifPresent(x->{if(!x.getId().equals(id)) throw new RuntimeException("Ya existe otro proveedor con ese NIT o documento");});
        aplicar(p,r); if(r.getActivo()!=null)p.setActivo(r.getActivo()); return repository.save(p);
    }

    @Transactional public void desactivar(Long id){Proveedor p=obtener(id);p.setActivo(false);repository.save(p);}

    public Map<String,Object> resumen(Long id){
        Proveedor p=obtener(id);
        Map<String,Object> m=new LinkedHashMap<>(); m.put("proveedor",p);
        Map<String,Object> stats=jdbc.queryForMap("""
            SELECT
              COALESCE((SELECT SUM(c.total) FROM compras c WHERE c.id_proveedor=? AND c.estado='registrada'),0) total_comprado,
              COALESCE((SELECT SUM(f.total_pagado) FROM facturas_proveedores f WHERE f.id_proveedor=? AND f.estado_pago<>'anulada'),0) total_pagado,
              COALESCE((SELECT SUM(f.saldo_pendiente) FROM facturas_proveedores f WHERE f.id_proveedor=? AND f.estado_pago IN ('pendiente','parcial')),0) saldo_pendiente,
              COALESCE((SELECT COUNT(*) FROM compras c WHERE c.id_proveedor=? AND c.estado='registrada'),0) numero_compras,
              COALESCE((SELECT COUNT(*) FROM facturas_proveedores f WHERE f.id_proveedor=? AND f.estado_pago IN ('pendiente','parcial')),0) facturas_pendientes
            """, id,id,id,id,id);
        m.putAll(stats); return m;
    }

    private void validar(ProveedorRequest r){
        if(r==null||vacio(r.getNitDocumento()))throw new RuntimeException("El NIT o documento es obligatorio");
        if(vacio(r.getRazonSocial()))throw new RuntimeException("La razón social es obligatoria");
        if(r.getCorreo()!=null&&!r.getCorreo().isBlank()&&!r.getCorreo().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$"))throw new RuntimeException("El correo no tiene un formato válido");
    }
    private void aplicar(Proveedor p,ProveedorRequest r){
        p.setNitDocumento(r.getNitDocumento().trim());p.setRazonSocial(r.getRazonSocial().trim());
        p.setContactoNombre(limpiar(r.getContactoNombre()));p.setTelefono(limpiar(r.getTelefono()));p.setCorreo(limpiar(r.getCorreo()));
        p.setDireccion(limpiar(r.getDireccion()));p.setCiudad(limpiar(r.getCiudad()));
        p.setCondicionesPago(vacio(r.getCondicionesPago())?"Contado":r.getCondicionesPago().trim());p.setObservaciones(limpiar(r.getObservaciones()));
    }
    private boolean vacio(String s){return s==null||s.isBlank();}
    private String limpiar(String s){return s==null||s.isBlank()?null:s.trim();}
}
