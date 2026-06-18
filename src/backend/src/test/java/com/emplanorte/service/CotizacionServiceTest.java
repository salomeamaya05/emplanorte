package com.emplanorte.service;

import com.emplanorte.dto.CotizacionRequest;
import com.emplanorte.dto.ItemCotizacionRequest;
import com.emplanorte.model.*;
import com.emplanorte.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CotizacionService — RF14")
class CotizacionServiceTest {

    @Mock private CotizacionRepository cotizacionRepository;
    @Mock private DetalleCotizacionRepository detalleCotizacionRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private VentaRepository ventaRepository;
    @Mock private DetalleVentaRepository detalleVentaRepository;

    @InjectMocks private CotizacionService cotizacionService;

    private Usuario usuario;
    private Cliente cliente;
    private Producto producto;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNombre("Duvan Alvarado");
        usuario.setActivo(true);

        cliente = new Cliente();
        cliente.setId(1L);
        cliente.setNombre("Distribuidora Norte");

        producto = new Producto();
        producto.setId(1L);
        producto.setNombre("Botella PET 500ml");
        producto.setStockDisponible(10);
        producto.setCostoUnitario(new BigDecimal("1000"));
        producto.setPrecioVenta(new BigDecimal("1500"));
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private CotizacionRequest buildRequest(List<ItemCotizacionRequest> detalles) {
        CotizacionRequest r = new CotizacionRequest();
        r.setIdUsuario(1L);
        r.setIdCliente(1L);
        r.setDetalles(detalles);
        return r;
    }

    private void stubGuardado() {
        when(cotizacionRepository.generarNumeroCotizacion()).thenReturn("COT-000001");
        when(cotizacionRepository.save(any(Cotizacion.class))).thenAnswer(inv -> {
            Cotizacion c = inv.getArgument(0);
            c.setId(1L);
            return c;
        });
        when(detalleCotizacionRepository.save(any(DetalleCotizacion.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private Cotizacion cotizacionPendiente() {
        Cotizacion c = new Cotizacion();
        c.setId(1L);
        c.setNumeroCotizacion("COT-000001");
        c.setEstado("pendiente");
        c.setCliente(cliente);
        c.setUsuario(usuario);
        c.setSubtotal(new BigDecimal("3000"));
        c.setDescuento(BigDecimal.ZERO);
        c.setTotal(new BigDecimal("3000"));
        return c;
    }

    private DetalleCotizacion detallePara(Cotizacion cot, Producto prod, int cantidad) {
        DetalleCotizacion d = new DetalleCotizacion();
        d.setCotizacion(cot);
        d.setProducto(prod);
        d.setCantidad(cantidad);
        d.setPrecioUnitario(prod.getPrecioVenta());
        d.setSubtotalLinea(prod.getPrecioVenta().multiply(new BigDecimal(cantidad)));
        return d;
    }

    // ─── CP-42  Registrar cotización exitosa ──────────────────────────────────

    @Test
    @DisplayName("CP-42: Cotización exitosa — número único, estado pendiente y fecha asignada")
    void registrarCotizacion_exitosa_numeroEstadoFechaCorrectos() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        stubGuardado();

        Cotizacion resultado = cotizacionService.registrarCotizacion(
                buildRequest(List.of(new ItemCotizacionRequest(1L, 3))));

        assertThat(resultado.getNumeroCotizacion()).isEqualTo("COT-000001");
        assertThat(resultado.getEstado()).isEqualTo("pendiente");
        assertThat(resultado.getFechaCotizacion()).isNotNull();
        assertThat(resultado.getCliente().getNombre()).isEqualTo("Distribuidora Norte");
    }

    @Test
    @DisplayName("CP-42b: Cálculo de total — precio $1500 × 3 unidades = $4500")
    void registrarCotizacion_calculaTotalCorrecto() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        stubGuardado();

        Cotizacion resultado = cotizacionService.registrarCotizacion(
                buildRequest(List.of(new ItemCotizacionRequest(1L, 3))));

        assertThat(resultado.getSubtotal()).isEqualByComparingTo(new BigDecimal("4500"));
        assertThat(resultado.getTotal()).isEqualByComparingTo(new BigDecimal("4500"));
    }

    @Test
    @DisplayName("CP-42c: Cotización con descuento — total = subtotal − descuento")
    void registrarCotizacion_conDescuento_totalDescontado() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        stubGuardado();

        CotizacionRequest req = buildRequest(List.of(new ItemCotizacionRequest(1L, 2))); // $3000
        req.setDescuento(new BigDecimal("500"));

        Cotizacion resultado = cotizacionService.registrarCotizacion(req);

        assertThat(resultado.getTotal()).isEqualByComparingTo(new BigDecimal("2500"));
    }

    @Test
    @DisplayName("CP-42d: Descuento mayor al subtotal — total queda en cero, nunca negativo")
    void registrarCotizacion_descuentoExcesivoTotalEnCero() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        stubGuardado();

        CotizacionRequest req = buildRequest(List.of(new ItemCotizacionRequest(1L, 1))); // $1500
        req.setDescuento(new BigDecimal("9999"));

        Cotizacion resultado = cotizacionService.registrarCotizacion(req);

        assertThat(resultado.getTotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ─── CP-43  Sin productos ─────────────────────────────────────────────────

    @Test
    @DisplayName("CP-43: Lista de productos vacía — cotización debe rechazarse")
    void registrarCotizacion_sinProductos_lanzaExcepcion() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));

        assertThatThrownBy(() ->
                cotizacionService.registrarCotizacion(buildRequest(Collections.emptyList()))
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("producto");

        // La validación ocurre antes de generar el número: no se desperdicia secuencia
        verify(cotizacionRepository, never()).generarNumeroCotizacion();
    }

    // ─── CP-44  Cotización NO descuenta stock ─────────────────────────────────

    @Test
    @DisplayName("CP-44: Cantidad > stock — cotización se permite, inventario no se toca")
    void registrarCotizacion_cantidadMayorAlStock_sePermiteSinDescontarInventario() {
        producto.setStockDisponible(3);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        stubGuardado();

        // Cotizar 50 unidades con solo 3 en stock → debe aceptarse
        Cotizacion resultado = cotizacionService.registrarCotizacion(
                buildRequest(List.of(new ItemCotizacionRequest(1L, 50))));

        assertThat(resultado).isNotNull();
        assertThat(producto.getStockDisponible()).isEqualTo(3); // sin cambios
        verify(productoRepository, never()).save(any());
    }

    // ─── Conversión a venta ───────────────────────────────────────────────────

    @Test
    @DisplayName("Convertir a venta exitosa — venta creada y cotización queda como 'convertida'")
    void convertirAVenta_exitosa_ventaCreadaYCotizacionMarcada() {
        Cotizacion cot = cotizacionPendiente();
        DetalleCotizacion det = detallePara(cot, producto, 2);

        when(cotizacionRepository.findById(1L)).thenReturn(Optional.of(cot));
        when(detalleCotizacionRepository.findByCotizacionId(1L)).thenReturn(List.of(det));
        when(ventaRepository.generarNumeroVenta()).thenReturn("VTA-000001");
        when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> { Venta v = inv.getArgument(0); v.setId(1L); return v; });
        when(detalleVentaRepository.save(any(DetalleVenta.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cotizacionRepository.save(any(Cotizacion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        Venta venta = cotizacionService.convertirAVenta(1L, "efectivo");

        assertThat(venta.getNumeroVenta()).isEqualTo("VTA-000001");
        assertThat(venta.getMetodoPago()).isEqualTo("efectivo");
        assertThat(cot.getEstado()).isEqualTo("convertida");
    }

    @Test
    @DisplayName("Convertir a venta — stock del producto se descuenta al convertir")
    void convertirAVenta_descuentaStockDelProducto() {
        producto.setStockDisponible(10);
        Cotizacion cot = cotizacionPendiente();
        DetalleCotizacion det = detallePara(cot, producto, 4);

        when(cotizacionRepository.findById(1L)).thenReturn(Optional.of(cot));
        when(detalleCotizacionRepository.findByCotizacionId(1L)).thenReturn(List.of(det));
        when(ventaRepository.generarNumeroVenta()).thenReturn("VTA-000002");
        when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> { Venta v = inv.getArgument(0); v.setId(2L); return v; });
        when(detalleVentaRepository.save(any(DetalleVenta.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cotizacionRepository.save(any(Cotizacion.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productoRepository.save(any(Producto.class))).thenAnswer(inv -> inv.getArgument(0));

        cotizacionService.convertirAVenta(1L, "transferencia");

        assertThat(producto.getStockDisponible()).isEqualTo(6); // 10 - 4
    }

    @Test
    @DisplayName("Convertir cotización ya convertida — RuntimeException con mensaje claro")
    void convertirAVenta_cotizacionYaConvertida_lanzaExcepcion() {
        Cotizacion cot = cotizacionPendiente();
        cot.setEstado("convertida");
        when(cotizacionRepository.findById(1L)).thenReturn(Optional.of(cot));

        assertThatThrownBy(() -> cotizacionService.convertirAVenta(1L, "efectivo"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("convertida");
    }

    @Test
    @DisplayName("Convertir — stock insuficiente al momento de convertir lanza excepción")
    void convertirAVenta_stockInsuficienteAlConvertir_lanzaExcepcion() {
        producto.setStockDisponible(2);
        Cotizacion cot = cotizacionPendiente();
        DetalleCotizacion det = detallePara(cot, producto, 5); // pide 5, hay 2

        when(cotizacionRepository.findById(1L)).thenReturn(Optional.of(cot));
        when(detalleCotizacionRepository.findByCotizacionId(1L)).thenReturn(List.of(det));
        when(ventaRepository.generarNumeroVenta()).thenReturn("VTA-000003");

        assertThatThrownBy(() -> cotizacionService.convertirAVenta(1L, "efectivo"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Stock insuficiente");

        // El stock no se alteró ante el fallo
        assertThat(producto.getStockDisponible()).isEqualTo(2);
    }

    // ─── Particiones de equivalencia ─────────────────────────────────────────

    @Test
    @DisplayName("Partición: cliente inexistente — RuntimeException descriptiva")
    void registrarCotizacion_clienteNoExiste_lanzaExcepcion() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(clienteRepository.findById(99L)).thenReturn(Optional.empty());

        CotizacionRequest req = buildRequest(List.of(new ItemCotizacionRequest(1L, 1)));
        req.setIdCliente(99L);

        assertThatThrownBy(() -> cotizacionService.registrarCotizacion(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Cliente");
    }

    @Test
    @DisplayName("Partición: cotización inexistente al convertir — RuntimeException descriptiva")
    void convertirAVenta_cotizacionNoExiste_lanzaExcepcion() {
        when(cotizacionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cotizacionService.convertirAVenta(99L, "efectivo"))
                .isInstanceOf(RuntimeException.class);
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PRUEBAS ADICIONALES (rol tester) — documentan cada respuesta del sistema
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("registrarCotizacion con producto inexistente — RuntimeException 'Producto no encontrado'")
    void registrarCotizacion_productoNoExiste_lanzaExcepcion() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(clienteRepository.findById(1L)).thenReturn(Optional.of(cliente));
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() ->
                cotizacionService.registrarCotizacion(buildRequest(List.of(new ItemCotizacionRequest(99L, 1)))))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Producto");
    }

}
