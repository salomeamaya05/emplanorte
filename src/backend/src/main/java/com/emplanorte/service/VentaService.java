package com.emplanorte.service;

import com.emplanorte.dto.ItemVentaRequest;
import com.emplanorte.dto.VentaEdicionRequest;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class VentaService {

    private static final ZoneId ZONA_BOGOTA = ZoneId.of("America/Bogota");
    private static final Set<String> METODOS_PAGO = Set.of(
            "efectivo", "transferencia", "tarjeta", "credito", "otro"
    );
    private static final DateTimeFormatter FORMATO_CAMBIO =
            DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a", new Locale("es", "CO"));

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

    @Autowired
    private CarteraService carteraService;

    public List<Venta> obtenerTodas() {
        return ventaRepository.findAll(
                Sort.by(
                        Sort.Order.desc("fechaVenta"),
                        Sort.Order.desc("id")
                )
        );
    }

    @Transactional
    public Venta registrarVenta(VentaRequest request) {
        if (request == null) {
            throw new RuntimeException("Los datos de la venta son obligatorios");
        }

        Usuario usuario = usuarioRepository.findById(request.getIdUsuario())
                .orElseThrow(() -> new RuntimeException("Usuario/Vendedor no encontrado"));
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new RuntimeException("El usuario está inactivo");
        }

        Cliente cliente = buscarCliente(request.getIdCliente());
        String metodoPago = validarMetodoPago(request.getMetodoPago());
        LocalDateTime fechaVenta = validarFechaVenta(request.getFechaVenta());
        Venta ventaOrigen = validarVentaOrigen(request);

        String numeroVenta = ventaRepository.generarNumeroVenta();

        Venta venta = new Venta();
        venta.setNumeroVenta(numeroVenta);
        venta.setCliente(cliente);
        venta.setUsuario(usuario);
        venta.setFechaVenta(fechaVenta);
        venta.setMetodoPago(metodoPago);
        venta.setDescuento(request.getDescuento() != null ? request.getDescuento() : BigDecimal.ZERO);
        venta.setEstado("completada");
        venta.setObservaciones(limpiarTexto(request.getObservaciones()));
        venta.setEditada(false);
        if (ventaOrigen != null) {
            venta.setIdVentaOrigen(ventaOrigen.getId());
        }

        BigDecimal subtotalAcumulado = BigDecimal.ZERO;
        BigDecimal costoAcumulado = BigDecimal.ZERO;
        List<DetalleVenta> detallesParaGuardar = new ArrayList<>();

        if (request.getDetalles() == null || request.getDetalles().isEmpty()) {
            throw new RuntimeException("Debe seleccionar al menos un producto para registrar la venta");
        }

        for (ItemVentaRequest item : request.getDetalles()) {
            if (item.getIdProducto() == null) {
                throw new RuntimeException("Debe seleccionar un producto en cada detalle de la venta");
            }
            if (item.getCantidad() == null || item.getCantidad() <= 0) {
                throw new RuntimeException("La cantidad de cada producto debe ser mayor a cero");
            }
        }

        // Bloquear siempre los productos en el mismo orden evita ventas simultáneas
        // sobre el mismo stock y reduce el riesgo de interbloqueos entre transacciones.
        Map<Long, Producto> productosBloqueados = new HashMap<>();
        List<Long> idsProducto = request.getDetalles().stream()
                .map(ItemVentaRequest::getIdProducto)
                .distinct()
                .sorted()
                .toList();
        for (Long idProducto : idsProducto) {
            Producto producto = productoRepository.buscarPorIdParaActualizar(idProducto)
                    .orElseThrow(() -> new RuntimeException(
                            "Producto con ID " + idProducto + " no existe"));
            productosBloqueados.put(idProducto, producto);
        }

        for (ItemVentaRequest item : request.getDetalles()) {
            Producto producto = productosBloqueados.get(item.getIdProducto());

            if (producto.getStockDisponible() < item.getCantidad()) {
                throw new RuntimeException("Stock insuficiente para el producto: " + producto.getNombre()
                        + ". Disponible: " + producto.getStockDisponible()
                        + ", Solicitado: " + item.getCantidad());
            }

            BigDecimal cantidadBD = new BigDecimal(item.getCantidad());
            BigDecimal precioUnitario = item.getPrecioUnitario() != null
                    && item.getPrecioUnitario().compareTo(BigDecimal.ZERO) > 0
                    ? item.getPrecioUnitario()
                    : producto.getPrecioVenta();
            BigDecimal costoUnitario = producto.getCostoUnitario();

            BigDecimal subtotalLinea = precioUnitario.multiply(cantidadBD);
            BigDecimal costoLinea = costoUnitario.multiply(cantidadBD);
            BigDecimal gananciaLinea = subtotalLinea.subtract(costoLinea);

            subtotalAcumulado = subtotalAcumulado.add(subtotalLinea);
            costoAcumulado = costoAcumulado.add(costoLinea);

            DetalleVenta detalle = new DetalleVenta();
            detalle.setProducto(producto);
            detalle.setCantidad(item.getCantidad());
            detalle.setPrecioUnitario(precioUnitario);
            detalle.setCostoUnitario(costoUnitario);
            detalle.setSubtotalLinea(subtotalLinea);
            detalle.setCostoLinea(costoLinea);
            detalle.setGananciaLinea(gananciaLinea);
            detallesParaGuardar.add(detalle);

            producto.setStockDisponible(producto.getStockDisponible() - item.getCantidad());
            productoRepository.save(producto);
        }

        venta.setSubtotal(subtotalAcumulado);
        BigDecimal totalFinal = subtotalAcumulado.subtract(venta.getDescuento());
        if (totalFinal.compareTo(BigDecimal.ZERO) < 0) {
            totalFinal = BigDecimal.ZERO;
        }
        venta.setTotal(totalFinal);
        venta.setTotalCosto(costoAcumulado);
        venta.setGanancia(totalFinal.subtract(costoAcumulado));

        Venta ventaGuardada = ventaRepository.save(venta);
        for (DetalleVenta detalle : detallesParaGuardar) {
            detalle.setVenta(ventaGuardada);
            detalleVentaRepository.save(detalle);
        }

        registrarAuditoria(ventaGuardada, "creacion", usuario, null, null, null);
        carteraService.crearParaVenta(ventaGuardada, request, usuario);

        if (ventaOrigen != null) {
            ventaOrigen.setIdVentaReemplazo(ventaGuardada.getId());
            ventaRepository.save(ventaOrigen);

            String motivo = exigirMotivo(request.getMotivoCorreccion());
            registrarAuditoria(
                    ventaOrigen,
                    "correccion_creada",
                    usuario,
                    motivo,
                    "La venta anulada fue reemplazada por " + ventaGuardada.getNumeroVenta(),
                    ventaGuardada.getId()
            );
            registrarAuditoria(
                    ventaGuardada,
                    "correccion_creada",
                    usuario,
                    motivo,
                    "Esta venta corrige a " + ventaOrigen.getNumeroVenta(),
                    ventaOrigen.getId()
            );
        }

        return ventaGuardada;
    }

    public List<DetalleVenta> obtenerDetalles(Long ventaId) {
        ventaRepository.findById(ventaId)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada"));
        return detalleVentaRepository.findByVentaId(ventaId);
    }

    /**
     * Edita únicamente datos administrativos. Productos, cantidades, precios,
     * descuento, costos, total y ganancia permanecen inmutables.
     */
    @Transactional
    public Venta editarDatosAdministrativos(Long ventaId, VentaEdicionRequest request) {
        if (request == null) {
            throw new RuntimeException("Los datos de edición son obligatorios");
        }

        Venta venta = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada"));
        if ("anulada".equalsIgnoreCase(venta.getEstado())) {
            throw new RuntimeException("Una venta anulada no se puede editar");
        }

        Usuario actor = validarAdministrador(request.getIdUsuario(), request.getContrasena());
        String motivo = exigirMotivo(request.getMotivo());
        Cliente clienteNuevo = buscarCliente(request.getIdCliente());
        LocalDateTime fechaNueva = validarFechaVenta(request.getFechaVenta());
        String metodoNuevo = validarMetodoPago(request.getMetodoPago());
        carteraService.validarCambioMetodoPago(venta, metodoNuevo);
        String observacionesNuevas = limpiarTexto(request.getObservaciones());

        List<String> cambios = new ArrayList<>();
        agregarCambio(cambios, "Cliente", nombreCliente(venta.getCliente()), nombreCliente(clienteNuevo));
        agregarCambio(cambios, "Fecha de venta", formatear(venta.getFechaVenta()), formatear(fechaNueva));
        agregarCambio(cambios, "Método de pago", venta.getMetodoPago(), metodoNuevo);
        agregarCambio(cambios, "Observaciones", limpiarTexto(venta.getObservaciones()), observacionesNuevas);

        if (cambios.isEmpty()) {
            throw new RuntimeException("No se detectaron cambios para guardar");
        }

        venta.setCliente(clienteNuevo);
        venta.setFechaVenta(fechaNueva);
        venta.setMetodoPago(metodoNuevo);
        venta.setObservaciones(observacionesNuevas);
        venta.setEditada(true);
        venta.setFechaUltimaEdicion(ahoraBogota());

        Venta guardada = ventaRepository.save(venta);
        registrarAuditoria(
                guardada,
                "edicion",
                actor,
                motivo,
                String.join("\n", cambios),
                null
        );
        return guardada;
    }

    /**
     * Anula una venta de forma segura y devuelve el stock. Si corregir=true,
     * el frontend abrirá después una nueva venta precargada y enlazada.
     */
    @Transactional
    public Venta anularVenta(
            Long ventaId,
            Long idUsuario,
            String contrasena,
            String motivo,
            boolean corregir
    ) {
        Venta venta = ventaRepository.findById(ventaId)
                .orElseThrow(() -> new RuntimeException("Venta no encontrada"));

        if ("anulada".equalsIgnoreCase(venta.getEstado())) {
            throw new RuntimeException("La venta ya fue anulada previamente");
        }

        Usuario usuario = validarAdministrador(idUsuario, contrasena);
        String motivoValidado = exigirMotivo(motivo);
        carteraService.validarAnulacion(venta);

        List<DetalleVenta> detalles = detalleVentaRepository.findByVentaId(ventaId);
        for (DetalleVenta detalle : detalles) {
            Producto producto = detalle.getProducto();
            producto.setStockDisponible(producto.getStockDisponible() + detalle.getCantidad());
            productoRepository.save(producto);
        }

        venta.setEstado("anulada");
        venta.setMotivoAnulacion(motivoValidado);
        Venta guardada = ventaRepository.save(venta);
        carteraService.anularPorVenta(guardada);

        String accion = corregir ? "anulacion_correccion" : "anulacion";
        String detalle = corregir
                ? "Inventario devuelto. La venta queda disponible para crear una venta corregida."
                : "Inventario devuelto al anular la venta.";
        registrarAuditoria(guardada, accion, usuario, motivoValidado, detalle, null);
        return guardada;
    }

    public List<AuditoriaVenta> obtenerAuditoria(Long ventaId) {
        return auditoriaVentaRepository.findByIdVentaOrderByFechaRegistroAsc(ventaId);
    }

    private Venta validarVentaOrigen(VentaRequest request) {
        if (request.getIdVentaOrigen() == null) {
            return null;
        }

        exigirMotivo(request.getMotivoCorreccion());
        Venta origen = ventaRepository.findById(request.getIdVentaOrigen())
                .orElseThrow(() -> new RuntimeException("La venta original no existe"));
        if (!"anulada".equalsIgnoreCase(origen.getEstado())) {
            throw new RuntimeException("La venta original debe estar anulada antes de crear la corrección");
        }
        if (origen.getIdVentaReemplazo() != null) {
            throw new RuntimeException("La venta original ya tiene una venta corregida asociada");
        }
        return origen;
    }

    private Cliente buscarCliente(Long idCliente) {
        if (idCliente == null) {
            return null;
        }
        return clienteRepository.findById(idCliente)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
    }

    private Usuario validarAdministrador(Long idUsuario, String contrasena) {
        if (idUsuario == null) {
            throw new RuntimeException("No se identificó al usuario que realiza la operación");
        }
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        if (!Boolean.TRUE.equals(usuario.getActivo())) {
            throw new RuntimeException("El usuario está inactivo");
        }
        String rol = usuario.getRol() == null ? "" : usuario.getRol().trim().toLowerCase();
        if (!Set.of("administrador", "superadmin", "admin").contains(rol)) {
            throw new RuntimeException("Solo un administrador puede modificar o anular ventas");
        }
        if (contrasena == null || !passwordEncoder.matches(contrasena, usuario.getContrasenaHash())) {
            throw new RuntimeException("Contraseña incorrecta. No se realizaron cambios.");
        }
        return usuario;
    }

    private LocalDateTime validarFechaVenta(LocalDateTime fechaSolicitada) {
        LocalDateTime ahora = ahoraBogota();
        LocalDateTime fecha = fechaSolicitada == null ? ahora : fechaSolicitada.withSecond(0).withNano(0);
        if (fecha.isAfter(ahora.plusMinutes(5))) {
            throw new RuntimeException("La fecha de la venta no puede estar en el futuro");
        }
        return fecha;
    }

    private String validarMetodoPago(String metodoPago) {
        String metodo = limpiarTexto(metodoPago);
        metodo = metodo == null ? "" : metodo.toLowerCase();
        if (!METODOS_PAGO.contains(metodo)) {
            throw new RuntimeException("Seleccione un método de pago válido");
        }
        return metodo;
    }

    private String exigirMotivo(String motivo) {
        String limpio = limpiarTexto(motivo);
        if (limpio == null || limpio.length() < 5) {
            throw new RuntimeException("Debe escribir un motivo claro de al menos 5 caracteres");
        }
        return limpio;
    }

    private String limpiarTexto(String valor) {
        if (valor == null) {
            return null;
        }
        String limpio = valor.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    private LocalDateTime ahoraBogota() {
        return LocalDateTime.now(ZONA_BOGOTA).withNano(0);
    }

    private String nombreCliente(Cliente cliente) {
        return cliente == null ? "Sin cliente" : cliente.getNombre();
    }

    private String formatear(LocalDateTime fecha) {
        return fecha == null ? "Sin fecha" : fecha.format(FORMATO_CAMBIO);
    }

    private void agregarCambio(List<String> cambios, String campo, Object anterior, Object nuevo) {
        if (!Objects.equals(anterior, nuevo)) {
            cambios.add(campo + ": " + valorVisible(anterior) + " → " + valorVisible(nuevo));
        }
    }

    private String valorVisible(Object valor) {
        if (valor == null || String.valueOf(valor).isBlank()) {
            return "(vacío)";
        }
        return String.valueOf(valor);
    }

    private void registrarAuditoria(
            Venta venta,
            String accion,
            Usuario actor,
            String motivo,
            String detalleCambios,
            Long idVentaRelacionada
    ) {
        AuditoriaVenta auditoria = new AuditoriaVenta();
        auditoria.setIdVenta(venta.getId());
        auditoria.setAccion(accion);
        auditoria.setNumeroVenta(venta.getNumeroVenta());
        auditoria.setTotal(venta.getTotal());
        auditoria.setEstado(venta.getEstado());
        auditoria.setMotivo(motivo);
        auditoria.setDetalleCambios(detalleCambios);
        auditoria.setIdVentaRelacionada(idVentaRelacionada);
        if (actor != null && actor.getId() != null) {
            auditoria.setIdUsuario(actor.getId());
            auditoria.setUsuarioNombre(actor.getNombre());
        }
        auditoriaVentaRepository.save(auditoria);
    }
}
