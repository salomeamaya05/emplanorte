package com.emplanorte.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.emplanorte.dto.CotizacionRequest;
import com.emplanorte.dto.ItemCotizacionRequest;
import com.emplanorte.model.Cliente;
import com.emplanorte.model.Cotizacion;
import com.emplanorte.model.DetalleCotizacion;
import com.emplanorte.model.DetalleVenta;
import com.emplanorte.model.Producto;
import com.emplanorte.model.Usuario;
import com.emplanorte.model.Venta;
import com.emplanorte.repository.ClienteRepository;
import com.emplanorte.repository.CotizacionRepository;
import com.emplanorte.repository.DetalleCotizacionRepository;
import com.emplanorte.repository.DetalleVentaRepository;
import com.emplanorte.repository.ProductoRepository;
import com.emplanorte.repository.UsuarioRepository;
import com.emplanorte.repository.VentaRepository;

@Service
public class CotizacionService {

    @Autowired
    private CotizacionRepository cotizacionRepository;

    @Autowired
    private DetalleCotizacionRepository detalleCotizacionRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private DetalleVentaRepository detalleVentaRepository;

    public List<Cotizacion> obtenerTodas() {
        return cotizacionRepository.findAll();
    }
    public List<DetalleCotizacion> obtenerDetalles(Long cotizacionId) {
    cotizacionRepository.findById(cotizacionId)
            .orElseThrow(() -> new RuntimeException("Cotización no encontrada"));

    return detalleCotizacionRepository.findByCotizacionId(cotizacionId);
}

    @Transactional
    public void eliminarCotizacion(Long id) {
        Cotizacion cotizacion = cotizacionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Cotización no encontrada"));

        if ("convertida".equals(cotizacion.getEstado())) {
            throw new RuntimeException("No se puede eliminar una cotización que ya fue convertida a venta");
        }

        detalleCotizacionRepository.deleteByCotizacionId(id);
        cotizacionRepository.deleteById(id);
    }

    @Transactional
    public Cotizacion registrarCotizacion(CotizacionRequest request) {
        Usuario usuario = usuarioRepository.findById(request.getIdUsuario())
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

        Cliente cliente = clienteRepository.findById(request.getIdCliente())
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

        if (request.getDetalles() == null || request.getDetalles().isEmpty()) {
            throw new RuntimeException("Debe agregar al menos un producto a la cotización");
        }

        String numeroCotizacion = cotizacionRepository.generarNumeroCotizacion();

        Cotizacion cotizacion = new Cotizacion();
        cotizacion.setNumeroCotizacion(numeroCotizacion);
        cotizacion.setCliente(cliente);
        cotizacion.setUsuario(usuario);
        cotizacion.setFechaCotizacion(LocalDateTime.now());
        cotizacion.setDescuento(request.getDescuento() != null ? request.getDescuento() : BigDecimal.ZERO);
        cotizacion.setEstado("pendiente");
        cotizacion.setNotas(request.getNotas());

        BigDecimal subtotalAcumulado = BigDecimal.ZERO;
        List<DetalleCotizacion> detallesParaGuardar = new ArrayList<>();

        for (ItemCotizacionRequest item : request.getDetalles()) {
            Producto producto = productoRepository.findById(item.getIdProducto())
                    .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

            // CP-44 - La cotización es una propuesta comercial: NO descuenta ni valida
            // el inventario. Se permiten cantidades mayores al stock disponible.
            // El stock solo se valida al CONVERTIR la cotización en venta.

            BigDecimal cantidadBD = new BigDecimal(item.getCantidad());
            BigDecimal precioUnitario = item.getPrecioUnitario() != null
        ? item.getPrecioUnitario()
        : producto.getPrecioVenta();
            BigDecimal subtotalLinea = precioUnitario.multiply(cantidadBD);

            subtotalAcumulado = subtotalAcumulado.add(subtotalLinea);

            DetalleCotizacion detalle = new DetalleCotizacion();
            detalle.setProducto(producto);
            detalle.setCantidad(item.getCantidad());
            detalle.setPrecioUnitario(precioUnitario);
            detalle.setSubtotalLinea(subtotalLinea);

            detallesParaGuardar.add(detalle);
        }

        cotizacion.setSubtotal(subtotalAcumulado);
        BigDecimal total = subtotalAcumulado.subtract(cotizacion.getDescuento());
        cotizacion.setTotal(total.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : total);

        Cotizacion cotizacionGuardada = cotizacionRepository.save(cotizacion);

        for (DetalleCotizacion det : detallesParaGuardar) {
            det.setCotizacion(cotizacionGuardada);
            detalleCotizacionRepository.save(det);
        }

        return cotizacionGuardada;
    }
    @Transactional
public Cotizacion actualizarCotizacion(Long id, CotizacionRequest request) {
    Cotizacion cotizacion = cotizacionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Cotización no encontrada"));

    if ("convertida".equals(cotizacion.getEstado())) {
        throw new RuntimeException("No se puede editar una cotización convertida a venta");
    }

    Cliente cliente = clienteRepository.findById(request.getIdCliente())
            .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));

    if (request.getDetalles() == null || request.getDetalles().isEmpty()) {
        throw new RuntimeException("Debe agregar al menos un producto a la cotización");
    }

    cotizacion.setCliente(cliente);
    cotizacion.setDescuento(request.getDescuento() != null ? request.getDescuento() : BigDecimal.ZERO);
    cotizacion.setNotas(request.getNotas());

    detalleCotizacionRepository.deleteByCotizacionId(id);

    BigDecimal subtotalAcumulado = BigDecimal.ZERO;

    for (ItemCotizacionRequest item : request.getDetalles()) {
        Producto producto = productoRepository.findById(item.getIdProducto())
                .orElseThrow(() -> new RuntimeException("Producto no encontrado"));

        // CP-44 - La cotización es una propuesta comercial: NO descuenta ni valida
        // el inventario. Se permiten cantidades mayores al stock disponible.
        // El stock solo se valida al CONVERTIR la cotización en venta.

        BigDecimal cantidadBD = new BigDecimal(item.getCantidad());
        BigDecimal precioUnitario = item.getPrecioUnitario() != null
                ? item.getPrecioUnitario()
                : producto.getPrecioVenta();

        BigDecimal subtotalLinea = precioUnitario.multiply(cantidadBD);
        subtotalAcumulado = subtotalAcumulado.add(subtotalLinea);

        DetalleCotizacion detalle = new DetalleCotizacion();
        detalle.setCotizacion(cotizacion);
        detalle.setProducto(producto);
        detalle.setCantidad(item.getCantidad());
        detalle.setPrecioUnitario(precioUnitario);
        detalle.setSubtotalLinea(subtotalLinea);

        detalleCotizacionRepository.save(detalle);
    }

    cotizacion.setSubtotal(subtotalAcumulado);
    BigDecimal total = subtotalAcumulado.subtract(cotizacion.getDescuento());
    cotizacion.setTotal(total.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : total);

    return cotizacionRepository.save(cotizacion);
}

    @Transactional
    public Venta convertirAVenta(Long cotizacionId, String metodoPago) {
        return convertirAVenta(cotizacionId, metodoPago, null);
    }

    @Transactional
    public Venta convertirAVenta(Long cotizacionId, String metodoPago, String referenciaPago) {
        Cotizacion cotizacion = cotizacionRepository.findById(cotizacionId)
                .orElseThrow(() -> new RuntimeException("Cotización no encontrada"));

        if ("convertida".equals(cotizacion.getEstado())) {
            throw new RuntimeException("La cotización ya fue convertida a venta previamente");
        }

        List<DetalleCotizacion> detallesCot = detalleCotizacionRepository.findByCotizacionId(cotizacionId);

        // Generar venta
        String numeroVenta = ventaRepository.generarNumeroVenta();

        // Observaciones: origen de la venta + referencia de la pasarela de pago (si aplica)
        String observaciones = "Generada automáticamente de la cotización " + cotizacion.getNumeroCotizacion();
        if (referenciaPago != null && !referenciaPago.isBlank()) {
            observaciones += " | Ref. pago: " + referenciaPago;
        }

        Venta venta = new Venta();
        venta.setNumeroVenta(numeroVenta);
        venta.setCliente(cotizacion.getCliente());
        venta.setUsuario(cotizacion.getUsuario());
        venta.setFechaVenta(LocalDateTime.now());
        venta.setSubtotal(cotizacion.getSubtotal());
        venta.setDescuento(cotizacion.getDescuento());
        venta.setTotal(cotizacion.getTotal());
        venta.setMetodoPago(metodoPago);
        venta.setEstado("completada");
        venta.setObservaciones(observaciones);

        BigDecimal totalCostoAcumulado = BigDecimal.ZERO;
        List<DetalleVenta> detallesVenta = new ArrayList<>();

        for (DetalleCotizacion detCot : detallesCot) {
            Producto producto = detCot.getProducto();
            
            // Validar Stock
            if (producto.getStockDisponible() < detCot.getCantidad()) {
                throw new RuntimeException("Stock insuficiente para el producto: " + producto.getNombre() 
                        + ". Disponible: " + producto.getStockDisponible() + ", Requerido: " + detCot.getCantidad());
            }

            BigDecimal cantidadBD = new BigDecimal(detCot.getCantidad());
            BigDecimal costoUnitario = producto.getCostoUnitario();
            BigDecimal costoLinea = costoUnitario.multiply(cantidadBD);
            BigDecimal gananciaLinea = detCot.getSubtotalLinea().subtract(costoLinea);

            totalCostoAcumulado = totalCostoAcumulado.add(costoLinea);

            DetalleVenta detVenta = new DetalleVenta();
            detVenta.setProducto(producto);
            detVenta.setCantidad(detCot.getCantidad());
            detVenta.setPrecioUnitario(detCot.getPrecioUnitario());
            detVenta.setCostoUnitario(costoUnitario);
            detVenta.setSubtotalLinea(detCot.getSubtotalLinea());
            detVenta.setCostoLinea(costoLinea);
            detVenta.setGananciaLinea(gananciaLinea);

            detallesVenta.add(detVenta);

            // Descontar Stock
            producto.setStockDisponible(producto.getStockDisponible() - detCot.getCantidad());
            productoRepository.save(producto);
        }

        venta.setTotalCosto(totalCostoAcumulado);
        venta.setGanancia(venta.getTotal().subtract(totalCostoAcumulado));

        Venta ventaGuardada = ventaRepository.save(venta);

        for (DetalleVenta detV : detallesVenta) {
            detV.setVenta(ventaGuardada);
            detalleVentaRepository.save(detV);
        }

        // Marcar cotización como convertida
        cotizacion.setEstado("convertida");
        cotizacionRepository.save(cotizacion);

        return ventaGuardada;
    }
}
