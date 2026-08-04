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
public class CompraService {
    private final CompraRepository compraRepo; private final DetalleCompraRepository detalleRepo;
    private final ProveedorRepository proveedorRepo; private final UsuarioRepository usuarioRepo;
    private final ProductoRepository productoRepo; private final FacturaProveedorRepository facturaRepo;
    private final PagoProveedorRepository pagoRepo; private final AuditoriaCompraRepository auditoriaRepo;
    private final PasswordEncoder encoder; private final FacturaProveedorService facturaService;

    public CompraService(CompraRepository c,DetalleCompraRepository d,ProveedorRepository p,UsuarioRepository u,
                         ProductoRepository pr,FacturaProveedorRepository f,PagoProveedorRepository pa,
                         AuditoriaCompraRepository a,PasswordEncoder e,FacturaProveedorService fs){
        compraRepo=c;detalleRepo=d;proveedorRepo=p;usuarioRepo=u;productoRepo=pr;facturaRepo=f;pagoRepo=pa;auditoriaRepo=a;encoder=e;facturaService=fs;
    }

    public List<Compra> listar(){return compraRepo.findAllByOrderByFechaCompraDescIdDesc();}
    public Compra obtener(Long id){return compraRepo.findById(id).orElseThrow(()->new RuntimeException("Compra no encontrada"));}
    public List<DetalleCompra> detalles(Long id){obtener(id);return detalleRepo.findByCompraIdOrderByIdAsc(id);}
    public List<AuditoriaCompra> auditoria(Long id){return auditoriaRepo.findByIdCompraOrderByFechaRegistroAsc(id);}

    @Transactional
    public Compra registrar(CompraRequest r){
        if(r==null||r.getIdProveedor()==null||r.getIdUsuario()==null)throw new RuntimeException("Proveedor y usuario son obligatorios");
        if(r.getDetalles()==null||r.getDetalles().isEmpty())throw new RuntimeException("Agregue al menos un producto a la compra");
        Proveedor proveedor=proveedorRepo.findById(r.getIdProveedor()).orElseThrow(()->new RuntimeException("Proveedor no encontrado"));
        if(!Boolean.TRUE.equals(proveedor.getActivo()))throw new RuntimeException("El proveedor está inactivo");
        Usuario usuario=usuarioRepo.findById(r.getIdUsuario()).orElseThrow(()->new RuntimeException("Usuario no encontrado"));

        Compra c=new Compra();
        c.setNumeroCompra(compraRepo.generarNumeroCompra());c.setProveedor(proveedor);c.setUsuario(usuario);
        c.setFechaCompra(r.getFechaCompra()!=null?r.getFechaCompra():LocalDateTime.now());
        c.setFlete(noNegativo(r.getFlete(),"El flete"));c.setImpuestos(noNegativo(r.getImpuestos(),"Los impuestos"));
        c.setDescuento(noNegativo(r.getDescuento(),"El descuento"));c.setObservaciones(limpiar(r.getObservaciones()));c.setEstado("registrada");

        List<ItemPreparado> preparados=new ArrayList<>();
        Set<Long> productosRepetidos=new HashSet<>();
        BigDecimal subtotal=BigDecimal.ZERO;
        for(ItemCompraRequest item:r.getDetalles()){
            if(item.getIdProducto()==null||item.getCantidad()==null||item.getCantidad()<=0)throw new RuntimeException("Cada producto debe tener una cantidad mayor a cero");
            if(!productosRepetidos.add(item.getIdProducto()))throw new RuntimeException("No repita el mismo producto en una compra; ajuste la cantidad en una sola línea");
            BigDecimal costo=noNegativo(item.getCostoUnitario(),"El costo unitario");
            if(costo.signum()<=0)throw new RuntimeException("El costo unitario debe ser mayor a cero");
            Producto producto=productoRepo.buscarPorIdParaActualizar(item.getIdProducto()).orElseThrow(()->new RuntimeException("Producto no encontrado"));
            if(!Boolean.TRUE.equals(producto.getActivo()))throw new RuntimeException("El producto "+producto.getNombre()+" está inactivo");
            BigDecimal subtotalLinea=costo.multiply(BigDecimal.valueOf(item.getCantidad())).setScale(2,RoundingMode.HALF_UP);
            preparados.add(new ItemPreparado(item,producto,costo,subtotalLinea));
            subtotal=subtotal.add(subtotalLinea);
        }

        BigDecimal total=subtotal.add(c.getFlete()).add(c.getImpuestos()).subtract(c.getDescuento()).setScale(2,RoundingMode.HALF_UP);
        if(total.signum()<0)throw new RuntimeException("El descuento no puede superar el subtotal más flete e impuestos");
        c.setSubtotal(subtotal);c.setTotal(total);

        List<DetalleCompra> detalles=new ArrayList<>();
        BigDecimal valorInventarioAsignado=BigDecimal.ZERO;
        for(int indice=0;indice<preparados.size();indice++){
            ItemPreparado x=preparados.get(indice);
            BigDecimal valorInventarioLinea;
            if(indice==preparados.size()-1){
                valorInventarioLinea=total.subtract(valorInventarioAsignado);
            }else{
                valorInventarioLinea=x.subtotalLinea.multiply(total).divide(subtotal,2,RoundingMode.HALF_UP);
                valorInventarioAsignado=valorInventarioAsignado.add(valorInventarioLinea);
            }
            if(valorInventarioLinea.signum()<0)valorInventarioLinea=BigDecimal.ZERO;
            BigDecimal costoInventario=valorInventarioLinea.divide(BigDecimal.valueOf(x.item.getCantidad()),2,RoundingMode.HALF_UP);

            int stockAnterior=Optional.ofNullable(x.producto.getStockDisponible()).orElse(0);
            BigDecimal costoAnterior=Optional.ofNullable(x.producto.getCostoUnitario()).orElse(BigDecimal.ZERO);
            int stockPosterior=stockAnterior+x.item.getCantidad();
            BigDecimal valorAnterior=costoAnterior.multiply(BigDecimal.valueOf(stockAnterior));
            BigDecimal valorNuevo=costoInventario.multiply(BigDecimal.valueOf(x.item.getCantidad()));
            BigDecimal costoPromedio=valorAnterior.add(valorNuevo).divide(BigDecimal.valueOf(stockPosterior),2,RoundingMode.HALF_UP);

            DetalleCompra d=new DetalleCompra();d.setProducto(x.producto);d.setCantidad(x.item.getCantidad());
            d.setCostoUnitario(x.costo);d.setCostoUnitarioInventario(costoInventario);d.setSubtotalLinea(x.subtotalLinea);
            d.setStockAnterior(stockAnterior);d.setCostoAnterior(costoAnterior);d.setStockPosterior(stockPosterior);d.setCostoPromedioPosterior(costoPromedio);
            detalles.add(d);
            x.producto.setStockDisponible(stockPosterior);x.producto.setCostoUnitario(costoPromedio);productoRepo.save(x.producto);
        }

        Compra guardada=compraRepo.save(c);
        for(DetalleCompra d:detalles){d.setCompra(guardada);detalleRepo.save(d);}
        guardada.setDetalles(detalles);auditar(guardada,"creacion",usuario,null);
        if(Boolean.TRUE.equals(r.getRegistrarFactura())){
            facturaService.crearDesdeCompra(guardada,r.getNumeroFactura(),r.getFechaEmision(),r.getFechaVencimiento(),r.getObservacionesFactura());
        }
        return guardada;
    }

    private static class ItemPreparado {
        private final ItemCompraRequest item;private final Producto producto;private final BigDecimal costo;private final BigDecimal subtotalLinea;
        private ItemPreparado(ItemCompraRequest item,Producto producto,BigDecimal costo,BigDecimal subtotalLinea){this.item=item;this.producto=producto;this.costo=costo;this.subtotalLinea=subtotalLinea;}
    }

    @Transactional
    public Compra anular(Long id,AnulacionRequest r){
        Compra c=compraRepo.buscarPorIdParaActualizar(id).orElseThrow(()->new RuntimeException("Compra no encontrada"));
        if("anulada".equals(c.getEstado()))throw new RuntimeException("La compra ya está anulada");
        Usuario u=validarIdentidad(r);
        if(r.getMotivo()==null||r.getMotivo().isBlank())throw new RuntimeException("Debe indicar el motivo de la anulación");
        Optional<FacturaProveedor> factura=facturaRepo.findByCompraId(id);
        if(factura.isPresent()&&pagoRepo.existsByFacturaIdAndEstado(factura.get().getId(),"activo"))throw new RuntimeException("No puede anular la compra porque su factura ya tiene pagos. Primero anule los pagos registrados.");

        List<DetalleCompra> detalles=detalleRepo.findByCompraIdOrderByIdAsc(id);
        for(DetalleCompra d:detalles){
            Producto p=productoRepo.buscarPorIdParaActualizar(d.getProducto().getId()).orElseThrow(()->new RuntimeException("Producto no encontrado"));
            boolean stockIgual=Objects.equals(p.getStockDisponible(),d.getStockPosterior());
            boolean costoIgual=p.getCostoUnitario()!=null&&p.getCostoUnitario().compareTo(d.getCostoPromedioPosterior())==0;
            if(!stockIgual||!costoIgual)throw new RuntimeException("No se puede anular: el inventario de '"+p.getNombre()+"' cambió después de la compra. Para conservar costos y stock correctos, realice un ajuste documentado en inventario.");
        }
        for(DetalleCompra d:detalles){Producto p=productoRepo.buscarPorIdParaActualizar(d.getProducto().getId()).orElseThrow();p.setStockDisponible(d.getStockAnterior());p.setCostoUnitario(d.getCostoAnterior());productoRepo.save(p);}
        c.setEstado("anulada");c.setMotivoAnulacion(r.getMotivo().trim());c.setAnuladoEn(LocalDateTime.now());Compra g=compraRepo.save(c);
        factura.ifPresent(f->{f.setEstadoPago("anulada");f.setSaldoPendiente(BigDecimal.ZERO);facturaRepo.save(f);});auditar(g,"anulacion",u,r.getMotivo());return g;
    }

    private Usuario validarIdentidad(AnulacionRequest r){
        if(r==null||r.getIdUsuario()==null)throw new RuntimeException("No se identificó al usuario");
        Usuario u=usuarioRepo.findById(r.getIdUsuario()).orElseThrow(()->new RuntimeException("Usuario no encontrado"));
        if(r.getContrasena()==null||!encoder.matches(r.getContrasena(),u.getContrasenaHash()))throw new RuntimeException("Contraseña incorrecta");return u;
    }
    private void auditar(Compra c,String accion,Usuario u,String motivo){AuditoriaCompra a=new AuditoriaCompra();a.setIdCompra(c.getId());a.setIdUsuario(u.getId());a.setUsuarioNombre(u.getNombre());a.setAccion(accion);a.setNumeroCompra(c.getNumeroCompra());a.setTotal(c.getTotal());a.setEstado(c.getEstado());a.setMotivo(motivo);auditoriaRepo.save(a);}
    private BigDecimal noNegativo(BigDecimal v,String nombre){BigDecimal x=v==null?BigDecimal.ZERO:v;if(x.signum()<0)throw new RuntimeException(nombre+" no puede ser negativo");return x.setScale(2,RoundingMode.HALF_UP);}
    private String limpiar(String s){return s==null||s.isBlank()?null:s.trim();}
}
