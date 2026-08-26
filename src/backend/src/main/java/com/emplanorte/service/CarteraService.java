package com.emplanorte.service;

import com.emplanorte.dto.*;
import com.emplanorte.model.*;
import com.emplanorte.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
public class CarteraService {

    private static final ZoneId ZONA_BOGOTA = ZoneId.of("America/Bogota");
    private static final Set<String> FORMAS_PAGO = Set.of(
            "efectivo", "transferencia", "nequi", "daviplata", "tarjeta", "otro"
    );
    private static final Set<String> ROLES_ADMIN = Set.of("administrador", "superadmin", "admin");

    private final CreditoVentaRepository creditoRepository;
    private final AbonoCreditoRepository abonoRepository;
    private final UsuarioRepository usuarioRepository;
    private final ClienteRepository clienteRepository;
    private final VentaRepository ventaRepository;
    private final PasswordEncoder passwordEncoder;

    public CarteraService(
            CreditoVentaRepository creditoRepository,
            AbonoCreditoRepository abonoRepository,
            UsuarioRepository usuarioRepository,
            ClienteRepository clienteRepository,
            VentaRepository ventaRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.creditoRepository = creditoRepository;
        this.abonoRepository = abonoRepository;
        this.usuarioRepository = usuarioRepository;
        this.clienteRepository = clienteRepository;
        this.ventaRepository = ventaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public CreditoVenta crearParaVenta(Venta venta, VentaRequest request, Usuario usuario) {
        if (!"credito".equalsIgnoreCase(venta.getMetodoPago())) {
            validarAusenciaDatosCredito(request);
            return null;
        }
        if (venta.getCliente() == null) {
            throw new RuntimeException("Una venta a crédito debe tener un cliente");
        }
        if (request.getFechaVencimientoCredito() == null) {
            throw new RuntimeException("Indique la fecha acordada de pago");
        }
        if (request.getFechaVencimientoCredito().isBefore(venta.getFechaVenta().toLocalDate())) {
            throw new RuntimeException("La fecha de pago no puede ser anterior a la venta");
        }
        if (creditoRepository.findByVentaId(venta.getId()).isPresent()) {
            throw new RuntimeException("La venta ya tiene un crédito asociado");
        }

        BigDecimal total = dinero(venta.getTotal());
        if (total.signum() <= 0) {
            throw new RuntimeException("Una venta a crédito debe tener un total mayor a cero");
        }
        BigDecimal pagoInicial = dinero(request.getPagoInicial());
        if (pagoInicial.signum() < 0) {
            throw new RuntimeException("El pago inicial no puede ser negativo");
        }
        if (pagoInicial.compareTo(total) > 0) {
            throw new RuntimeException("El pago inicial no puede superar el total de la venta");
        }

        CreditoVenta credito = new CreditoVenta();
        credito.setVenta(venta);
        credito.setCliente(venta.getCliente());
        credito.setTotalCredito(total);
        credito.setSaldoPendiente(total.subtract(pagoInicial));
        credito.setFechaVencimiento(request.getFechaVencimientoCredito());
        credito.setEstado(credito.getSaldoPendiente().signum() == 0 ? "pagado" : "pendiente");
        credito.setObservaciones(limpiar(request.getObservacionesCredito()));
        CreditoVenta guardado = creditoRepository.save(credito);

        if (pagoInicial.signum() > 0) {
            String forma = validarFormaPago(request.getFormaPagoInicial());
            AbonoCredito inicial = new AbonoCredito();
            inicial.setCredito(guardado);
            inicial.setUsuario(usuario);
            inicial.setMonto(pagoInicial);
            inicial.setFormaPago(forma);
            inicial.setFechaPago(venta.getFechaVenta());
            inicial.setTipo("inicial");
            inicial.setObservaciones("Pago inicial registrado con la venta");
            inicial.setClaveIdempotencia("INICIAL-VENTA-" + venta.getId());
            abonoRepository.save(inicial);
        }
        return guardado;
    }

    public List<CreditoVentaResponse> listar(String estado, String buscar) {
        String estadoFiltro = normalizar(estado);
        String texto = normalizar(buscar);
        return creditoRepository.findAllByOrderByFechaVencimientoAscIdDesc().stream()
                .map(CreditoVentaResponse::desde)
                .filter(c -> estadoFiltro.isBlank()
                        || "todos".equals(estadoFiltro)
                        || c.getEstado().equals(estadoFiltro))
                .filter(c -> texto.isBlank() || coincide(c, texto))
                .toList();
    }

    public CreditoVentaResponse obtener(Long id) {
        return CreditoVentaResponse.desde(buscar(id));
    }

    public CreditoVentaResponse obtenerPorVenta(Long idVenta) {
        return CreditoVentaResponse.desde(creditoRepository.findByVentaId(idVenta)
                .orElseThrow(() -> new RuntimeException("La venta no tiene un crédito asociado")));
    }

    public List<AbonoCreditoResponse> listarAbonos(Long idCredito) {
        buscar(idCredito);
        return abonoRepository.findByCreditoIdOrderByFechaPagoAscIdAsc(idCredito).stream()
                .map(AbonoCreditoResponse::desde)
                .toList();
    }

    @Transactional
    public AbonoCreditoResponse registrarAbono(Long idCredito, AbonoCreditoRequest request) {
        if (request == null) throw new RuntimeException("Los datos del abono son obligatorios");
        String clave = limpiar(request.getClaveIdempotencia());
        if (clave == null || clave.length() < 12 || clave.length() > 80) {
            throw new RuntimeException("No fue posible identificar de forma segura este abono");
        }

        Optional<AbonoCredito> existente = abonoRepository.findByClaveIdempotencia(clave);
        if (existente.isPresent()) return validarIdempotencia(existente.get(), idCredito);

        CreditoVenta credito = creditoRepository.buscarPorIdParaActualizar(idCredito)
                .orElseThrow(() -> new RuntimeException("Crédito no encontrado"));
        existente = abonoRepository.findByClaveIdempotencia(clave);
        if (existente.isPresent()) return validarIdempotencia(existente.get(), idCredito);

        if (!"pendiente".equalsIgnoreCase(credito.getEstado())) {
            throw new RuntimeException("Este crédito no admite nuevos abonos porque está " + credito.getEstado());
        }
        if (!"completada".equalsIgnoreCase(credito.getVenta().getEstado())) {
            throw new RuntimeException("No se pueden registrar abonos sobre una venta anulada");
        }

        BigDecimal monto = dinero(request.getMonto());
        if (monto.signum() <= 0) throw new RuntimeException("El abono debe ser mayor a cero");
        if (monto.compareTo(credito.getSaldoPendiente()) > 0) {
            throw new RuntimeException("El abono no puede superar el saldo pendiente de "
                    + credito.getSaldoPendiente().setScale(2, RoundingMode.HALF_UP));
        }

        Usuario usuario = usuarioActivo(request.getIdUsuario());
        LocalDateTime fecha = request.getFechaPago() == null ? ahora() : request.getFechaPago().withNano(0);
        if (fecha.isAfter(ahora().plusMinutes(5))) {
            throw new RuntimeException("La fecha del abono no puede estar en el futuro");
        }

        AbonoCredito abono = new AbonoCredito();
        abono.setCredito(credito);
        abono.setUsuario(usuario);
        abono.setMonto(monto);
        abono.setFormaPago(validarFormaPago(request.getFormaPago()));
        abono.setFechaPago(fecha);
        abono.setTipo("abono");
        abono.setObservaciones(limpiar(request.getObservaciones()));
        abono.setClaveIdempotencia(clave);
        AbonoCredito guardado = abonoRepository.save(abono);

        credito.setSaldoPendiente(credito.getSaldoPendiente().subtract(monto));
        if (credito.getSaldoPendiente().signum() == 0) credito.setEstado("pagado");
        creditoRepository.save(credito);
        return AbonoCreditoResponse.desde(guardado);
    }

    @Transactional
    public CreditoVentaResponse actualizar(Long idCredito, CreditoActualizacionRequest request) {
        if (request == null || request.getFechaVencimiento() == null) {
            throw new RuntimeException("La fecha acordada de pago es obligatoria");
        }
        CreditoVenta credito = creditoRepository.buscarPorIdParaActualizar(idCredito)
                .orElseThrow(() -> new RuntimeException("Crédito no encontrado"));
        validarAdministrador(request.getIdUsuario(), request.getContrasena());
        if (!"pendiente".equalsIgnoreCase(credito.getEstado())) {
            throw new RuntimeException("Solo se pueden editar créditos pendientes");
        }
        if (request.getFechaVencimiento().isBefore(credito.getVenta().getFechaVenta().toLocalDate())) {
            throw new RuntimeException("La fecha de pago no puede ser anterior a la venta");
        }
        credito.setFechaVencimiento(request.getFechaVencimiento());
        credito.setObservaciones(limpiar(request.getObservaciones()));
        return CreditoVentaResponse.desde(creditoRepository.save(credito));
    }

    public CarteraResumenResponse resumen() {
        List<CreditoVentaResponse> activos = creditoRepository.findAllByOrderByFechaVencimientoAscIdDesc()
                .stream().map(CreditoVentaResponse::desde)
                .filter(c -> Set.of("pendiente", "vencido", "vence_hoy").contains(c.getEstado()))
                .toList();
        BigDecimal porCobrar = activos.stream().map(CreditoVentaResponse::getSaldoPendiente)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal vencido = activos.stream().filter(c -> "vencido".equals(c.getEstado()))
                .map(CreditoVentaResponse::getSaldoPendiente).reduce(BigDecimal.ZERO, BigDecimal::add);
        long vencidos = activos.stream().filter(c -> "vencido".equals(c.getEstado())).count();
        long hoy = activos.stream().filter(c -> "vence_hoy".equals(c.getEstado())).count();
        long clientes = activos.stream().map(c -> c.getCliente().getId()).distinct().count();
        return new CarteraResumenResponse(
                porCobrar, vencido, porCobrar.subtract(vencido),
                (long) activos.size(), vencidos, hoy, clientes
        );
    }

    public ClienteCarteraResumenResponse resumenCliente(Long idCliente) {
        Cliente cliente = clienteRepository.findById(idCliente)
                .orElseThrow(() -> new RuntimeException("Cliente no encontrado"));
        List<Venta> ventas = ventaRepository.findByClienteIdAndEstado(idCliente, "completada");
        BigDecimal totalVentas = ventas.stream().map(Venta::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<CreditoVentaResponse> creditos = creditoRepository
                .findByClienteIdOrderByFechaVencimientoDesc(idCliente).stream()
                .map(CreditoVentaResponse::desde).toList();
        BigDecimal totalCredito = creditos.stream()
                .filter(c -> !"anulado".equals(c.getEstado()))
                .map(CreditoVentaResponse::getTotalCredito).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal abonadoCredito = creditos.stream()
                .filter(c -> !"anulado".equals(c.getEstado()))
                .map(CreditoVentaResponse::getTotalAbonado).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal contado = totalVentas.subtract(totalCredito);
        BigDecimal saldo = creditos.stream()
                .filter(c -> Set.of("pendiente", "vencido", "vence_hoy").contains(c.getEstado()))
                .map(CreditoVentaResponse::getSaldoPendiente).reduce(BigDecimal.ZERO, BigDecimal::add);
        long vencidos = creditos.stream().filter(c -> "vencido".equals(c.getEstado())).count();
        return new ClienteCarteraResumenResponse(
                cliente, totalVentas, contado.add(abonadoCredito), saldo, vencidos, creditos
        );
    }

    public void validarCambioMetodoPago(Venta venta, String metodoNuevo) {
        boolean eraCredito = "credito".equalsIgnoreCase(venta.getMetodoPago());
        boolean seraCredito = "credito".equalsIgnoreCase(metodoNuevo);
        if (eraCredito != seraCredito) {
            throw new RuntimeException(
                    "No se puede convertir una venta entre contado y crédito desde la edición administrativa. "
                            + "Use Anular y corregir.");
        }
    }

    public void validarAnulacion(Venta venta) {
        creditoRepository.findByVentaId(venta.getId()).ifPresent(credito -> {
            BigDecimal abonado = credito.getTotalCredito().subtract(credito.getSaldoPendiente());
            if (abonado.signum() > 0) {
                throw new RuntimeException(
                        "La venta tiene pagos registrados por " + abonado.setScale(2, RoundingMode.HALF_UP)
                                + ". No se puede anular hasta gestionar la devolución del dinero.");
            }
        });
    }

    @Transactional
    public void anularPorVenta(Venta venta) {
        creditoRepository.findByVentaId(venta.getId()).ifPresent(credito -> {
            credito.setEstado("anulado");
            creditoRepository.save(credito);
        });
    }

    private void validarAusenciaDatosCredito(VentaRequest request) {
        if (dinero(request.getPagoInicial()).signum() != 0
                || request.getFechaVencimientoCredito() != null
                || limpiar(request.getObservacionesCredito()) != null
                || limpiar(request.getFormaPagoInicial()) != null) {
            throw new RuntimeException("Los datos de cartera solo corresponden a una venta a crédito");
        }
    }

    private CreditoVenta buscar(Long id) {
        return creditoRepository.findOneById(id)
                .orElseThrow(() -> new RuntimeException("Crédito no encontrado"));
    }

    private AbonoCreditoResponse validarIdempotencia(AbonoCredito existente, Long idCredito) {
        if (!Objects.equals(existente.getCredito().getId(), idCredito)) {
            throw new RuntimeException("La identificación del abono ya fue usada en otro crédito");
        }
        return AbonoCreditoResponse.desde(existente);
    }

    private Usuario usuarioActivo(Long idUsuario) {
        if (idUsuario == null) throw new RuntimeException("No se identificó al usuario que registra el abono");
        Usuario usuario = usuarioRepository.findById(idUsuario)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));
        if (!Boolean.TRUE.equals(usuario.getActivo())) throw new RuntimeException("El usuario está inactivo");
        return usuario;
    }

    private Usuario validarAdministrador(Long idUsuario, String contrasena) {
        Usuario usuario = usuarioActivo(idUsuario);
        String rol = normalizar(usuario.getRol());
        if (!ROLES_ADMIN.contains(rol)) throw new RuntimeException("Solo un administrador puede editar el crédito");
        if (contrasena == null || !passwordEncoder.matches(contrasena, usuario.getContrasenaHash())) {
            throw new RuntimeException("Contraseña incorrecta. No se realizaron cambios.");
        }
        return usuario;
    }

    private String validarFormaPago(String formaPago) {
        String forma = normalizar(formaPago);
        if (!FORMAS_PAGO.contains(forma)) throw new RuntimeException("Seleccione una forma de pago válida");
        return forma;
    }

    private boolean coincide(CreditoVentaResponse credito, String texto) {
        String cliente = credito.getCliente() == null ? "" : credito.getCliente().getNombre();
        String telefono = credito.getCliente() == null ? "" : credito.getCliente().getTelefono();
        String documento = credito.getCliente() == null ? "" : credito.getCliente().getDocumento();
        return normalizar(cliente + " " + documento + " " + telefono + " "
                + credito.getNumeroVenta() + " " + credito.getFechaVenta() + " "
                + credito.getFechaVencimiento()).contains(texto);
    }

    private BigDecimal dinero(BigDecimal valor) {
        return (valor == null ? BigDecimal.ZERO : valor).setScale(2, RoundingMode.HALF_UP);
    }

    private String limpiar(String texto) {
        if (texto == null) return null;
        String limpio = texto.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    private String normalizar(String texto) {
        if (texto == null) return "";
        return Normalizer.normalize(texto.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT);
    }

    private LocalDateTime ahora() {
        return LocalDateTime.now(ZONA_BOGOTA).withNano(0);
    }
}
