package com.emplanorte.service;

import com.emplanorte.dto.ItemVentaRequest;
import com.emplanorte.dto.VentaRequest;
import com.emplanorte.model.*;
import com.emplanorte.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("VentaService — RF05, RF06, RF07, RF08")
class VentaServiceTest {

    @Mock private VentaRepository ventaRepository;
    @Mock private DetalleVentaRepository detalleVentaRepository;
    @Mock private ProductoRepository productoRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private AuditoriaVentaRepository auditoriaVentaRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private VentaService ventaService;

    private Usuario usuario;
    private Producto producto;

    @BeforeEach
    void setUp() {
        usuario = new Usuario();
        usuario.setId(1L);
        usuario.setNombre("Duvan Alvarado");
        usuario.setActivo(true);

        producto = new Producto();
        producto.setId(1L);
        producto.setNombre("Botella PET 500ml");
        producto.setStockDisponible(10);
        producto.setCostoUnitario(new BigDecimal("1000"));
        producto.setPrecioVenta(new BigDecimal("1500"));
    }

    // ─── Helpers ─────────────────────────────────────────────────────────────

    private VentaRequest buildRequest(List<ItemVentaRequest> detalles) {
        VentaRequest r = new VentaRequest();
        r.setIdUsuario(1L);
        r.setMetodoPago("efectivo");
        r.setDetalles(detalles);
        return r;
    }

    private void stubGuardado() {
        when(ventaRepository.generarNumeroVenta()).thenReturn("VTA-000001");
        when(ventaRepository.save(any(Venta.class))).thenAnswer(inv -> inv.getArgument(0));
        when(detalleVentaRepository.save(any(DetalleVenta.class))).thenAnswer(inv -> inv.getArgument(0));
        when(auditoriaVentaRepository.save(any(AuditoriaVenta.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ─── RF05 · RF06  Registro y selección de productos ───────────────────────

    @Test
    @DisplayName("CP-13: Venta exitosa con 1 producto — stock se descuenta de 10 a 7")
    void registrarVenta_exitosa_descuentaStock() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        stubGuardado();

        Venta resultado = ventaService.registrarVenta(buildRequest(List.of(new ItemVentaRequest(1L, 3))));

        assertThat(resultado.getNumeroVenta()).isEqualTo("VTA-000001");
        assertThat(producto.getStockDisponible()).isEqualTo(7);
    }

    @Test
    @DisplayName("CP-14: Cantidad mayor al stock — RuntimeException con mensaje claro")
    void registrarVenta_stockInsuficiente_lanzaExcepcion() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(ventaRepository.generarNumeroVenta()).thenReturn("VTA-000002");

        assertThatThrownBy(() ->
                ventaService.registrarVenta(buildRequest(List.of(new ItemVentaRequest(1L, 15))))
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Stock insuficiente");

        // El stock no debe haberse alterado
        assertThat(producto.getStockDisponible()).isEqualTo(10);
    }

    @Test
    @DisplayName("CP-15: Venta con lista de productos vacía — debe rechazarse")
    void registrarVenta_sinProductos_lanzaExcepcion() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(ventaRepository.generarNumeroVenta()).thenReturn("VTA-000003");

        assertThatThrownBy(() ->
                ventaService.registrarVenta(buildRequest(Collections.emptyList()))
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("producto");
    }

    @Test
    @DisplayName("CP-16: Venta con múltiples productos — todos los stocks se actualizan")
    void registrarVenta_multiplesProductos_actualizaTodosLosStocks() {
        Producto producto2 = new Producto();
        producto2.setId(2L);
        producto2.setNombre("Botella Ámbar 250ml");
        producto2.setStockDisponible(20);
        producto2.setCostoUnitario(new BigDecimal("800"));
        producto2.setPrecioVenta(new BigDecimal("1200"));

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(productoRepository.findById(2L)).thenReturn(Optional.of(producto2));
        stubGuardado();

        ventaService.registrarVenta(buildRequest(List.of(
                new ItemVentaRequest(1L, 2),   // stock 10 → 8
                new ItemVentaRequest(2L, 5)    // stock 20 → 15
        )));

        assertThat(producto.getStockDisponible()).isEqualTo(8);
        assertThat(producto2.getStockDisponible()).isEqualTo(15);
    }

    @Test
    @DisplayName("CP-17: Método de pago registrado correctamente en la venta")
    void registrarVenta_metodoPagoQuedaraGuardado() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        stubGuardado();

        VentaRequest req = buildRequest(List.of(new ItemVentaRequest(1L, 1)));
        req.setMetodoPago("transferencia");
        Venta resultado = ventaService.registrarVenta(req);

        assertThat(resultado.getMetodoPago()).isEqualTo("transferencia");
    }

    // ─── RF07  Cálculos financieros ───────────────────────────────────────────

    @Test
    @DisplayName("CP-20: Total = precio $1500 × cantidad 3 = $4500")
    void registrarVenta_calculaTotalUnProducto() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        stubGuardado();

        Venta resultado = ventaService.registrarVenta(buildRequest(List.of(new ItemVentaRequest(1L, 3))));

        assertThat(resultado.getSubtotal()).isEqualByComparingTo(new BigDecimal("4500"));
        assertThat(resultado.getTotal()).isEqualByComparingTo(new BigDecimal("4500"));
    }

    @Test
    @DisplayName("CP-21: Ganancia = (precio $1500 − costo $1000) × 2 unidades = $1000")
    void registrarVenta_calculaGananciaLineal() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        stubGuardado();

        Venta resultado = ventaService.registrarVenta(buildRequest(List.of(new ItemVentaRequest(1L, 2))));

        assertThat(resultado.getGanancia()).isEqualByComparingTo(new BigDecimal("1000"));
    }

    @Test
    @DisplayName("CP-22: Total múltiples productos — A: $1000×2 + B: $500×3 = $3500")
    void registrarVenta_calculaTotalMultiplesProductos() {
        Producto prodA = new Producto();
        prodA.setId(1L);
        prodA.setStockDisponible(20);
        prodA.setCostoUnitario(new BigDecimal("700"));
        prodA.setPrecioVenta(new BigDecimal("1000"));

        Producto prodB = new Producto();
        prodB.setId(2L);
        prodB.setStockDisponible(20);
        prodB.setCostoUnitario(new BigDecimal("300"));
        prodB.setPrecioVenta(new BigDecimal("500"));

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(prodA));
        when(productoRepository.findById(2L)).thenReturn(Optional.of(prodB));
        stubGuardado();

        Venta resultado = ventaService.registrarVenta(buildRequest(List.of(
                new ItemVentaRequest(1L, 2),   // $1000 × 2 = $2000
                new ItemVentaRequest(2L, 3)    // $500  × 3 = $1500
        )));

        assertThat(resultado.getTotal()).isEqualByComparingTo(new BigDecimal("3500"));
    }

    @Test
    @DisplayName("CP-23: Precio decimal $1250.50 × 4 = $5002.00 sin pérdida de precisión")
    void registrarVenta_preciosDecimales_sinPerdidaDePrecision() {
        producto.setPrecioVenta(new BigDecimal("1250.50"));
        producto.setCostoUnitario(new BigDecimal("900.25"));

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        stubGuardado();

        Venta resultado = ventaService.registrarVenta(buildRequest(List.of(new ItemVentaRequest(1L, 4))));

        assertThat(resultado.getTotal()).isEqualByComparingTo(new BigDecimal("5002.00"));
    }

    // ─── RF08  Actualización de inventario ───────────────────────────────────

    @Test
    @DisplayName("CP-24: Stock inicial 20 − 5 unidades vendidas = stock final 15")
    void registrarVenta_descuentaStockExacto() {
        producto.setStockDisponible(20);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        stubGuardado();

        ventaService.registrarVenta(buildRequest(List.of(new ItemVentaRequest(1L, 5))));

        assertThat(producto.getStockDisponible()).isEqualTo(15);
    }

    @Test
    @DisplayName("CP-25: Venta que agota el stock — producto queda en 0 y la venta se completa")
    void registrarVenta_agotaStock_productoNoDesaparece() {
        producto.setStockDisponible(5);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        stubGuardado();

        Venta resultado = ventaService.registrarVenta(buildRequest(List.of(new ItemVentaRequest(1L, 5))));

        assertThat(producto.getStockDisponible()).isEqualTo(0);
        assertThat(resultado).isNotNull();
        assertThat(resultado.getEstado()).isEqualTo("completada");
    }

    // ─── Particiones de equivalencia ─────────────────────────────────────────

    @Test
    @DisplayName("Partición: cantidad cero — debe rechazarse")
    void registrarVenta_cantidadCero_lanzaExcepcion() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(ventaRepository.generarNumeroVenta()).thenReturn("VTA-000011");

        assertThatThrownBy(() ->
                ventaService.registrarVenta(buildRequest(List.of(new ItemVentaRequest(1L, 0))))
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("mayor a cero");
    }

    @Test
    @DisplayName("Partición: cantidad negativa — debe rechazarse")
    void registrarVenta_cantidadNegativa_lanzaExcepcion() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(ventaRepository.generarNumeroVenta()).thenReturn("VTA-000012");

        assertThatThrownBy(() ->
                ventaService.registrarVenta(buildRequest(List.of(new ItemVentaRequest(1L, -5))))
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("mayor a cero");
    }

    @Test
    @DisplayName("Partición: usuario inexistente — excepción con mensaje descriptivo")
    void registrarVenta_usuarioNoExiste_lanzaExcepcion() {
        VentaRequest req = buildRequest(List.of(new ItemVentaRequest(1L, 1)));
        req.setIdUsuario(99L);
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ventaService.registrarVenta(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Usuario");
    }

    @Test
    @DisplayName("Partición: producto inexistente — excepción con ID del producto")
    void registrarVenta_productoNoExiste_lanzaExcepcion() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(productoRepository.findById(99L)).thenReturn(Optional.empty());
        when(ventaRepository.generarNumeroVenta()).thenReturn("VTA-000013");

        assertThatThrownBy(() ->
                ventaService.registrarVenta(buildRequest(List.of(new ItemVentaRequest(99L, 1))))
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("99");
    }

    // ════════════════════════════════════════════════════════════════════════
    //  PRUEBAS ADICIONALES (rol tester) — documentan cada respuesta del sistema
    // ════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("Venta SIN cliente (idCliente null) — se registra igual con cliente nulo")
    void registrarVenta_sinCliente_seRegistraConClienteNulo() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        stubGuardado();

        Venta resultado = ventaService.registrarVenta(buildRequest(List.of(new ItemVentaRequest(1L, 1))));

        assertThat(resultado.getCliente()).isNull();
        assertThat(resultado.getEstado()).isEqualTo("completada");
    }

    @Test
    @DisplayName("Venta con idCliente que NO existe — RuntimeException 'Cliente no encontrado'")
    void registrarVenta_clienteNoExiste_lanzaExcepcion() {
        VentaRequest req = buildRequest(List.of(new ItemVentaRequest(1L, 1)));
        req.setIdCliente(50L);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(clienteRepository.findById(50L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ventaService.registrarVenta(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Cliente no encontrado");
    }

    @Test
    @DisplayName("Venta exitosa — la fecha de venta se asigna automáticamente (no nula)")
    void registrarVenta_fechaSeAsignaAutomaticamente() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        stubGuardado();

        Venta resultado = ventaService.registrarVenta(buildRequest(List.of(new ItemVentaRequest(1L, 1))));

        assertThat(resultado.getFechaVenta()).isNotNull();
    }

    @Test
    @DisplayName("Venta con descuento — total = subtotal − descuento; ganancia = total − costo")
    void registrarVenta_conDescuento_totalYGananciaCorrectos() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        stubGuardado();

        VentaRequest req = buildRequest(List.of(new ItemVentaRequest(1L, 2))); // subtotal 3000, costo 2000
        req.setDescuento(new BigDecimal("500"));

        Venta resultado = ventaService.registrarVenta(req);

        assertThat(resultado.getSubtotal()).isEqualByComparingTo(new BigDecimal("3000"));
        assertThat(resultado.getTotal()).isEqualByComparingTo(new BigDecimal("2500"));      // 3000 - 500
        assertThat(resultado.getGanancia()).isEqualByComparingTo(new BigDecimal("500"));    // 2500 - 2000
    }

    @Test
    @DisplayName("Descuento mayor al subtotal — el total se limita a cero, nunca negativo")
    void registrarVenta_descuentoExcesivo_totalEnCero() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        stubGuardado();

        VentaRequest req = buildRequest(List.of(new ItemVentaRequest(1L, 1))); // subtotal 1500
        req.setDescuento(new BigDecimal("9999"));

        Venta resultado = ventaService.registrarVenta(req);

        assertThat(resultado.getTotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("CP-21 adicional: venta a PÉRDIDA (precio < costo) — ganancia queda negativa")
    void registrarVenta_aPerdida_gananciaNegativa() {
        producto.setPrecioVenta(new BigDecimal("800"));
        producto.setCostoUnitario(new BigDecimal("1000"));
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        stubGuardado();

        Venta resultado = ventaService.registrarVenta(buildRequest(List.of(new ItemVentaRequest(1L, 2))));

        // total 1600 - costo 2000 = -400
        assertThat(resultado.getGanancia()).isNegative();
        assertThat(resultado.getGanancia()).isEqualByComparingTo(new BigDecimal("-400"));
    }

    @Test
    @DisplayName("Partición 'muy grande' (999999999) con stock 10 — rechazada por stock insuficiente")
    void registrarVenta_cantidadMuyGrande_rechazadaPorStock() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(productoRepository.findById(1L)).thenReturn(Optional.of(producto));
        when(ventaRepository.generarNumeroVenta()).thenReturn("VTA-000099");

        assertThatThrownBy(() ->
                ventaService.registrarVenta(buildRequest(List.of(new ItemVentaRequest(1L, 999999999))))
        )
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Stock insuficiente");
        assertThat(producto.getStockDisponible()).isEqualTo(10); // intacto
    }
}
