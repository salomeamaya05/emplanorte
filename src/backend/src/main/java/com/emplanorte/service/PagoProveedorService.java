package com.emplanorte.service;

import com.emplanorte.dto.*;
import com.emplanorte.model.*;
import com.emplanorte.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class PagoProveedorService {
    private final PagoProveedorRepository pagoRepo;private final FacturaProveedorRepository facturaRepo;private final UsuarioRepository usuarioRepo;
    private final AuditoriaPagoProveedorRepository auditRepo;private final PasswordEncoder encoder;
    public PagoProveedorService(PagoProveedorRepository p,FacturaProveedorRepository f,UsuarioRepository u,AuditoriaPagoProveedorRepository a,PasswordEncoder e){pagoRepo=p;facturaRepo=f;usuarioRepo=u;auditRepo=a;encoder=e;}
    public List<PagoProveedor> listar(Long idFactura){return pagoRepo.findByFacturaIdOrderByFechaPagoDescIdDesc(idFactura);}

    @Transactional
    public PagoProveedor registrar(Long idFactura,PagoProveedorRequest r){
        if(r==null||r.getIdUsuario()==null)throw new RuntimeException("El usuario es obligatorio");
        FacturaProveedor f=facturaRepo.buscarPorIdParaActualizar(idFactura).orElseThrow(()->new RuntimeException("Factura no encontrada"));
        if("anulada".equals(f.getEstadoPago()))throw new RuntimeException("No se pueden registrar pagos en una factura anulada");
        if("pagada".equals(f.getEstadoPago()))throw new RuntimeException("La factura ya está pagada");
        BigDecimal monto=r.getMonto()==null?BigDecimal.ZERO:r.getMonto().setScale(2,RoundingMode.HALF_UP);
        if(monto.signum()<=0)throw new RuntimeException("El monto debe ser mayor a cero");
        if(monto.compareTo(f.getSaldoPendiente())>0)throw new RuntimeException("El pago supera el saldo pendiente de "+f.getSaldoPendiente());
        if(!Set.of("efectivo","transferencia","tarjeta","otro").contains(r.getMetodoPago()))throw new RuntimeException("Método de pago inválido");
        Usuario u=usuarioRepo.findById(r.getIdUsuario()).orElseThrow(()->new RuntimeException("Usuario no encontrado"));
        PagoProveedor p=new PagoProveedor();p.setFactura(f);p.setUsuario(u);p.setFechaPago(r.getFechaPago()!=null?r.getFechaPago():LocalDateTime.now());p.setMonto(monto);p.setMetodoPago(r.getMetodoPago());p.setReferencia(limpiar(r.getReferencia()));p.setObservaciones(limpiar(r.getObservaciones()));p.setEstado("activo");
        PagoProveedor g=pagoRepo.save(p);recalcularFactura(f,monto,true);auditar(g,"creacion",u,null);return g;
    }

    @Transactional
    public PagoProveedor anular(Long idPago,AnulacionRequest r){
        PagoProveedor p=pagoRepo.buscarPorIdParaActualizar(idPago).orElseThrow(()->new RuntimeException("Pago no encontrado"));
        if("anulado".equals(p.getEstado()))throw new RuntimeException("El pago ya está anulado");
        Usuario u=validar(r);if(r.getMotivo()==null||r.getMotivo().isBlank())throw new RuntimeException("Debe indicar el motivo");
        FacturaProveedor f=facturaRepo.buscarPorIdParaActualizar(p.getFactura().getId()).orElseThrow();
        p.setEstado("anulado");p.setMotivoAnulacion(r.getMotivo().trim());p.setAnuladoEn(LocalDateTime.now());PagoProveedor g=pagoRepo.save(p);
        recalcularFactura(f,p.getMonto(),false);auditar(g,"anulacion",u,r.getMotivo());return g;
    }

    private void recalcularFactura(FacturaProveedor f,BigDecimal monto,boolean sumar){
        BigDecimal pagado=sumar?f.getTotalPagado().add(monto):f.getTotalPagado().subtract(monto);if(pagado.signum()<0)pagado=BigDecimal.ZERO;
        BigDecimal saldo=f.getTotalFactura().subtract(pagado);if(saldo.signum()<0)saldo=BigDecimal.ZERO;
        f.setTotalPagado(pagado);f.setSaldoPendiente(saldo);f.setEstadoPago(saldo.signum()==0?"pagada":pagado.signum()==0?"pendiente":"parcial");facturaRepo.save(f);
    }
    private Usuario validar(AnulacionRequest r){if(r==null||r.getIdUsuario()==null)throw new RuntimeException("Usuario requerido");Usuario u=usuarioRepo.findById(r.getIdUsuario()).orElseThrow(()->new RuntimeException("Usuario no encontrado"));if(r.getContrasena()==null||!encoder.matches(r.getContrasena(),u.getContrasenaHash()))throw new RuntimeException("Contraseña incorrecta");return u;}
    private void auditar(PagoProveedor p,String accion,Usuario u,String motivo){AuditoriaPagoProveedor a=new AuditoriaPagoProveedor();a.setIdPago(p.getId());a.setIdFactura(p.getFactura().getId());a.setIdUsuario(u.getId());a.setUsuarioNombre(u.getNombre());a.setAccion(accion);a.setMonto(p.getMonto());a.setMotivo(motivo);auditRepo.save(a);}
    private String limpiar(String s){return s==null||s.isBlank()?null:s.trim();}
}
