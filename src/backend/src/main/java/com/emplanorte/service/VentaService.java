package com.emplanorte.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.emplanorte.dto.ItemVentaRequest;
import com.emplanorte.dto.VentaRequest;
import com.emplanorte.model.AuditoriaVenta;
import com.emplanorte.model.Cliente;
import com.emplanorte.model.DetalleVenta;
import com.emplanorte.model.Producto;
import com.emplanorte.model.Usuario;
import com.emplanorte.model.Venta;
import com.emplanorte.repository.AuditoriaVentaRepository;
import com.emplanorte.repository.ClienteRepository;
import com.emplanorte.repository.DetalleVentaRepository;
import com.emplanorte.repository.ProductoRepository;
import com.emplanorte.repository.UsuarioRepository;
import com.emplanorte.repository.VentaRepository;

@Service
public class VentaService {

    @Autowired
    private VentaRepository ventaRepository;

    @Autowired
    private DetalleVentaRepository detalleVentaRepository;

    @Autowired
    private ProductoRepository productoRepository;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AuditoriaVentaRepository auditoriaVentaRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<Venta> obtenerTodas() {
        return ventaRepository.findAll();
    }

    @Transactional
    public Venta registrarVenta(VentaRequest request) {
        // 1. Validaciones iniciales
        Usuario usuario = usuarioRepository.findById(request.getIdUsuario())
                .orElseThrow(() -> new RuntimeException("Usuario/Vendedor no encontrado"));

        Cliente cliente = null;
        if (request.getIdCliente() != null) {
            cliente = clienteRepository.findById(request.getIdCliente())
                    .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        }

        // Generar número correlativo llamando a la función de PostgreSQL
        String numeroVenta = ventaRepository.generarNumeroVenta();

        // 2. Instanciar cabecera de la venta
        Venta venta = new Venta();
        venta.setNumeroVenta(numeroVenta);
        venta.setCliente(cliente);
        venta.setUsuario(usuario);
        venta.setFechaVenta(LocalDateTime.now());
        venta.setMetodoPago(request.getMetodoPago());
        venta.setDescuento(request.getDescuento() != null ? request.getDescuento() : BigDecimal.ZERO);
        venta.setEstado("completada");
        venta.setObservaciones(request.getObservaciones());

        // Inicializar acumuladores financieros
        BigDecimal subtotalAcumulado = BigDecimal.ZERO;
        BigDecimal costoAcumulado = BigDecimal.ZERO;
        BigDecimal gananciaAcumulada = BigDecimal.ZERO;

        List<DetalleVenta> detallesParaGuardar = new ArrayList<>();

        // 3. Procesar ítems de la venta (RF06, RF07, RF08)
        if (request.getDetalles() == null || request.getDetalles().isEmpty()) {
            throw new RuntimeException("Debe seleccionar al menos un producto para registrar la venta");
        }

        for (ItemVentaRequest item : request.getDetalles()) {
            if (item.getCantidad() == null || item.getCantidad() <= 0) {
                throw new RuntimeException("La cantidad de cada producto debe ser mayor a cero");
            }

            Producto producto = productoRepository.findById(item.getIdProducto())
                    .orElseThrow(() -> new RuntimeException("Producto con ID " + item.getIdProducto() + " no existe"));

            // Validar stock disponible (RF08)
            if (producto.getStockDisponible() < item.getCantidad()) {
                throw new RuntimeException("Stock insuficiente para el producto: " + producto.getNombre() 
                        + ". Disponible: " + producto.getStockDisponible() + ", Solicitado: " + item.getCantidad());
            }

            // Calcular valores de la línea (RF07)
            BigDecimal cantidadBD = new BigDecimal(item.getCantidad());
            BigDecimal precioUnitario = producto.getPrecioVenta();
            BigDecimal costoUnitario = producto.getCostoUnitario();

            BigDecimal subtotalLinea = precioUnitario.multiply(cantidadBD);
            BigDecimal costoLinea = costoUnitario.multiply(cantidadBD);
            BigDecimal gananciaLinea = subtotalLinea.subtract(costoLinea);

            // Acumular totales de la venta
            subtotalAcumulado = subtotalAcumulado.add(subtotalLinea);
            costoAcumulado = costoAcumulado.add(costoLinea);
            gananciaAcumulada = gananciaAcumulada.add(gananciaLinea);

            // Instanciar detalle
            DetalleVenta detalle = new DetalleVenta();
            detalle.setProducto(producto);
            detalle.setCantidad(item.getCantidad());
            detalle.setPrecioUnitario(precioUnitario);
            detalle.setCostoUnitario(costoUnitario);
            detalle.setSubtotalLinea(subtotalLinea);
            detalle.setCostoLinea(costoLinea);
            detalle.setGananciaLinea(gananciaLinea);

            detallesParaGuardar.add(detalle);
            
            // Restar stock del producto (RF08)
            // Nota: Aunque PostgreSQL tiene un trigger para esto en la BD, también lo actualizamos en Java
            // para mantener coherencia en el estado de memoria antes del commit/flush
            producto.setStockDisponible(producto.getStockDisponible() - item.getCantidad());
            productoRepository.save(producto);
        }

        // 4. Completar cabecera con montos acumulados
        venta.setSubtotal(subtotalAcumulado);
        
        // El total es subtotal menos el descuento general aplicado
        BigDecimal totalFinal = subtotalAcumulado.subtract(venta.getDescuento());
        if (totalFinal.compareTo(BigDecimal.ZERO) < 0) {
            totalFinal = BigDecimal.ZERO;
        }
        venta.setTotal(totalFinal);
        
        venta.setTotalCosto(costoAcumulado);
        
        // La ganancia real de la venta es el total recaudado menos el costo total de los productos
        venta.setGanancia(totalFinal.subtract(costoAcumulado));

        // 5. Guardar Venta
        Venta ventaGuardada = ventaRepository.save(venta);

        // 6. Guardar detalles asociados
        for (DetalleVenta det : detallesParaGuardar) {
            det.setVenta(ventaGuardada);
            detalleVentaRepository.save(det);
        }

        registrarAuditoria(ventaGuardada, "creacion", usuario);

        return ventaGuardada;
    }
    public List<DetalleVenta> obtenerDetalles(Long ventaId) {
    ventaRepository.findById(ventaId)
            .orElseThrow(() -> new RuntimeException("Venta no encontrada"));

    return detalleVentaRepository.findByVentaId(ventaId);
}

    /**
     * Anula una venta de forma segura: verifica la contraseña del usuario que la
     * ejecuta, devuelve el stock de los productos al inventario y deja registro
     * en la bitácora de auditoría. Las ventas no se editan: se anulan y se crea
     * una nueva corregida.
     */
    @Transactional
    public Venta anularVenta(Long ventaId, Long idUsuario, String contrasena) {
        Venta venta = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada"));

        if ("anulada".equals(venta.getEstado())) {
            throw new RuntimeException("La venta ya fue anulada previamente");
        }

        // Verificación de identidad: contraseña del usuario actual (BCrypt)
        if (idUsuario == null) {
            throw new RuntimeException("No se identificó al usuario que solicita la anulación");
        }
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        if (contrasena == null || !passwordEncoder.matches(contrasena, usuario.getContrasenaHash())) {
            throw new RuntimeException("Contraseña incorrecta. La venta no fue anulada.");
        }

        // Devolver el stock de cada producto al inventario
        List<DetalleVenta> detalles = detalleVentaRepository.findByVentaId(ventaId);
        for (DetalleVenta det : detalles) {
            Producto producto = det.getProducto();
            producto.setStockDisponible(producto.getStockDisponible() + det.getCantidad());
            productoRepository.save(producto);
        }

        venta.setEstado("anulada");
        Venta guardada = ventaRepository.save(venta);

        registrarAuditoria(guardada, "anulacion", usuario);
        return guardada;
    }

    // Historial de auditoría de una venta (creación / anulación)
    public List<AuditoriaVenta> obtenerAuditoria(Long ventaId) {
        return auditoriaVentaRepository.findByIdVentaOrderByFechaRegistroAsc(ventaId);
    }

    // Guarda un registro de auditoría con un snapshot del estado de la venta.
    private void registrarAuditoria(Venta venta, String accion, Usuario actor) {
        AuditoriaVenta a = new AuditoriaVenta();
        a.setIdVenta(venta.getId());
        a.setAccion(accion);
        a.setNumeroVenta(venta.getNumeroVenta());
        a.setTotal(venta.getTotal());
        a.setEstado(venta.getEstado());
        if (actor != null && actor.getId() != null) {
            a.setIdUsuario(actor.getId());
            a.setUsuarioNombre(actor.getNombre());
        }
        auditoriaVentaRepository.save(a);
    }
}
