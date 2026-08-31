package com.emplanorte.integration;

import com.emplanorte.dto.AbonoCreditoRequest;
import com.emplanorte.dto.AnulacionRequest;
import com.emplanorte.dto.CompraRequest;
import com.emplanorte.dto.DashboardFinancieroResponse;
import com.emplanorte.dto.ItemCompraRequest;
import com.emplanorte.dto.ItemVentaRequest;
import com.emplanorte.dto.VentaRequest;
import com.emplanorte.model.CategoriaProducto;
import com.emplanorte.model.Cliente;
import com.emplanorte.model.Compra;
import com.emplanorte.model.CreditoVenta;
import com.emplanorte.model.DetalleCompra;
import com.emplanorte.model.Producto;
import com.emplanorte.model.Proveedor;
import com.emplanorte.model.Usuario;
import com.emplanorte.model.Venta;
import com.emplanorte.repository.AbonoCreditoRepository;
import com.emplanorte.repository.CategoriaProductoRepository;
import com.emplanorte.repository.ClienteRepository;
import com.emplanorte.repository.CreditoVentaRepository;
import com.emplanorte.repository.ProductoRepository;
import com.emplanorte.repository.ProveedorRepository;
import com.emplanorte.repository.UsuarioRepository;
import com.emplanorte.service.CarteraService;
import com.emplanorte.service.CompraService;
import com.emplanorte.service.DashboardService;
import com.emplanorte.service.VentaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("local-e2e")
@Transactional
@DisplayName("Flujos integrados sobre la base local aislada")
class FlujosLocalesIntegrationTest {

    @Autowired private CompraService compraService;
    @Autowired private VentaService ventaService;
    @Autowired private CarteraService carteraService;
    @Autowired private DashboardService dashboardService;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private CategoriaProductoRepository categoriaRepository;
    @Autowired private ProductoRepository productoRepository;
    @Autowired private ClienteRepository clienteRepository;
    @Autowired private ProveedorRepository proveedorRepository;
    @Autowired private CreditoVentaRepository creditoRepository;
    @Autowired private AbonoCreditoRepository abonoRepository;
    @Autowired private PasswordEncoder passwordEncoder;

    private Usuario usuario;
    private CategoriaProducto categoria;
    private Cliente cliente;
    private Proveedor proveedor;

    @BeforeEach
    void prepararDatosFicticios() {
        usuario = new Usuario();
        usuario.setNombre("Admin integración local");
        usuario.setCorreo("integracion.local@emplanorte.test");
        usuario.setContrasenaHash(passwordEncoder.encode("ClaveLocal2026!"));
        usuario.setRol("administrador");
        usuario.setActivo(true);
        usuario = usuarioRepository.saveAndFlush(usuario);

        categoria = new CategoriaProducto();
        categoria.setNombre("Categoría integración local");
        categoria.setDescripcion("Datos ficticios");
        categoria.setActivo(true);
        categoria = categoriaRepository.saveAndFlush(categoria);

        cliente = new Cliente();
        cliente.setNombre("Cliente integración local");
        cliente.setDocumento("LOCAL-E2E-1001");
        cliente.setTelefono("3000000010");
        cliente.setActivo(true);
        cliente = clienteRepository.saveAndFlush(cliente);

        proveedor = new Proveedor();
        proveedor.setNitDocumento("LOCAL-E2E-9001");
        proveedor.setRazonSocial("Proveedor integración local");
        proveedor.setCondicionesPago("Contado");
        proveedor.setActivo(true);
        proveedor = proveedorRepository.saveAndFlush(proveedor);
    }

    @Test
    @DisplayName("Compra por pacas, venta y anulación conservan flete, costo y stock exactos")
    void compraVentaYAnulacion_conservanInventario() {
        Producto grande = producto("E2E-5L", "Envase 5 litros", 32, "2000", "4200", 100);
        Producto pequeno = producto("E2E-150", "Envase 150 ml", 150, "500", "900", 0);

        CompraRequest solicitud = new CompraRequest();
        solicitud.setIdProveedor(proveedor.getId());
        solicitud.setIdUsuario(usuario.getId());
        solicitud.setFechaCompra(LocalDateTime.now().minusHours(1));
        solicitud.setFlete(new BigDecimal("100000"));
        solicitud.setImpuestos(BigDecimal.ZERO);
        solicitud.setDescuento(BigDecimal.ZERO);
        solicitud.setRegistrarFactura(false);
        solicitud.setDetalles(List.of(
                itemCompra(grande.getId(), 10, 32, "2450"),
                itemCompra(pequeno.getId(), 15, 150, "500")
        ));

        Compra compra = compraService.registrar(solicitud);
        List<DetalleCompra> detalles = compraService.detalles(compra.getId());

        assertThat(detalles).hasSize(2);
        assertThat(detalles.get(0).getCantidad()).isEqualTo(320);
        assertThat(detalles.get(1).getCantidad()).isEqualTo(2250);
        assertThat(detalles.get(0).getFleteAsignado()).isEqualByComparingTo("40000.00");
        assertThat(detalles.get(1).getFleteAsignado()).isEqualByComparingTo("60000.00");
        assertThat(detalles.get(0).getCostoUnitarioInventario()).isEqualByComparingTo("2575.00");
        assertThat(detalles.get(1).getCostoUnitarioInventario()).isEqualByComparingTo("526.67");
        assertThat(detalles.stream().map(DetalleCompra::getFleteAsignado)
                .reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("100000.00");

        Producto grandeComprado = productoRepository.findById(grande.getId()).orElseThrow();
        Producto pequenoComprado = productoRepository.findById(pequeno.getId()).orElseThrow();
        assertThat(grandeComprado.getStockDisponible()).isEqualTo(420);
        assertThat(grandeComprado.getCostoUnitario()).isEqualByComparingTo("2438.10");
        assertThat(pequenoComprado.getStockDisponible()).isEqualTo(2250);

        Venta venta = ventaService.registrarVenta(venta(
                grande.getId(), 20, "4200", "efectivo", null, null
        ));
        assertThat(productoRepository.findById(grande.getId()).orElseThrow().getStockDisponible())
                .isEqualTo(400);

        ventaService.anularVenta(
                venta.getId(), usuario.getId(), "ClaveLocal2026!",
                "Prueba integrada de devolución", false
        );
        assertThat(productoRepository.findById(grande.getId()).orElseThrow().getStockDisponible())
                .isEqualTo(420);
    }

    @Test
    @DisplayName("Una compra repite el producto en presentaciones distintas y la anulación restaura el inventario")
    void compraConProductoRepetido_conservaLineasYSeAnulaEnOrdenInverso() {
        Producto producto = producto("E2E-REP", "Producto en varias pacas", 24, "1000", "2500", 50);

        CompraRequest solicitud = new CompraRequest();
        solicitud.setIdProveedor(proveedor.getId());
        solicitud.setIdUsuario(usuario.getId());
        solicitud.setFechaCompra(LocalDateTime.now().minusHours(1));
        solicitud.setFlete(new BigDecimal("9000"));
        solicitud.setImpuestos(BigDecimal.ZERO);
        solicitud.setDescuento(BigDecimal.ZERO);
        solicitud.setRegistrarFactura(false);
        solicitud.setDetalles(List.of(
                itemCompra(producto.getId(), 2, 24, "1000"),
                itemCompra(producto.getId(), 1, 12, "1200")
        ));

        Compra compra = compraService.registrar(solicitud);
        List<DetalleCompra> detalles = compraService.detalles(compra.getId());

        assertThat(detalles).hasSize(2);
        assertThat(detalles).extracting(d -> d.getProducto().getId())
                .containsExactly(producto.getId(), producto.getId());
        assertThat(detalles).extracting(DetalleCompra::getCantidadPacas).containsExactly(2, 1);
        assertThat(detalles).extracting(DetalleCompra::getUnidadesPorPaca).containsExactly(24, 12);
        assertThat(detalles).extracting(DetalleCompra::getCantidad).containsExactly(48, 12);
        assertThat(detalles.get(0).getFleteAsignado()).isEqualByComparingTo("6000.00");
        assertThat(detalles.get(1).getFleteAsignado()).isEqualByComparingTo("3000.00");
        assertThat(detalles.get(0).getStockAnterior()).isEqualTo(50);
        assertThat(detalles.get(0).getStockPosterior()).isEqualTo(98);
        assertThat(detalles.get(1).getStockAnterior()).isEqualTo(98);
        assertThat(detalles.get(1).getStockPosterior()).isEqualTo(110);
        assertThat(productoRepository.findById(producto.getId()).orElseThrow().getStockDisponible())
                .isEqualTo(110);
        assertThat(productoRepository.findById(producto.getId()).orElseThrow().getCostoUnitario())
                .isEqualByComparingTo("1103.63");

        AnulacionRequest anulacion = new AnulacionRequest();
        anulacion.setIdUsuario(usuario.getId());
        anulacion.setContrasena("ClaveLocal2026!");
        anulacion.setMotivo("Anulación ficticia de compra repetida");
        compraService.anular(compra.getId(), anulacion);

        Producto restaurado = productoRepository.findById(producto.getId()).orElseThrow();
        assertThat(restaurado.getStockDisponible()).isEqualTo(50);
        assertThat(restaurado.getCostoUnitario()).isEqualByComparingTo("1000.00");
    }

    @Test
    @DisplayName("Crédito separa venta y recaudo, evita doble abono y jamás mueve el stock al cobrar")
    void creditoYAbonos_mantienenTrazabilidad() {
        Producto producto = producto("E2E-CRED", "Producto crédito", 10, "2000", "100000", 100);
        Venta venta = ventaService.registrarVenta(venta(
                producto.getId(), 10, "100000", "credito",
                new BigDecimal("300000"), LocalDate.now().plusDays(30)
        ));
        CreditoVenta credito = creditoRepository.findByVentaId(venta.getId()).orElseThrow();

        assertThat(venta.getTotal()).isEqualByComparingTo("1000000.00");
        assertThat(credito.getSaldoPendiente()).isEqualByComparingTo("700000.00");
        assertThat(abonoRepository.findByCreditoIdOrderByFechaPagoAscIdAsc(credito.getId()))
                .singleElement().extracting("tipo").isEqualTo("inicial");
        assertThat(productoRepository.findById(producto.getId()).orElseThrow().getStockDisponible())
                .isEqualTo(90);

        AbonoCreditoRequest parcial = abono("200000", "integracion-abono-0001");
        Long idPrimerAbono = carteraService.registrarAbono(credito.getId(), parcial).getId();
        Long idRepetido = carteraService.registrarAbono(credito.getId(), parcial).getId();

        assertThat(idRepetido).isEqualTo(idPrimerAbono);
        assertThat(creditoRepository.findById(credito.getId()).orElseThrow().getSaldoPendiente())
                .isEqualByComparingTo("500000.00");
        assertThat(abonoRepository.findByCreditoIdOrderByFechaPagoAscIdAsc(credito.getId()))
                .hasSize(2);
        assertThat(productoRepository.findById(producto.getId()).orElseThrow().getStockDisponible())
                .isEqualTo(90);

        DashboardFinancieroResponse balance = dashboardService.obtenerBalanceCompleto(
                LocalDate.now(), LocalDate.now()
        );
        assertThat(balance.getVentasNetas()).isEqualByComparingTo("1000000.00");
        assertThat(balance.getRecaudoVentasPeriodo()).isEqualByComparingTo("500000.00");
        assertThat(balance.getCuentasPorCobrar()).isEqualByComparingTo("500000.00");

        assertThatThrownBy(() -> carteraService.registrarAbono(
                credito.getId(), abono("500000.01", "integracion-excesivo-01")
        )).hasMessageContaining("no puede superar");

        carteraService.registrarAbono(
                credito.getId(), abono("500000", "integracion-abono-final")
        );
        CreditoVenta pagado = creditoRepository.findById(credito.getId()).orElseThrow();
        assertThat(pagado.getSaldoPendiente()).isEqualByComparingTo("0.00");
        assertThat(pagado.getEstado()).isEqualTo("pagado");
        assertThat(venta.getTotal()).isEqualByComparingTo("1000000.00");
        assertThat(productoRepository.findById(producto.getId()).orElseThrow().getStockDisponible())
                .isEqualTo(90);
    }

    private Producto producto(
            String codigo,
            String nombre,
            int unidadesPorPaca,
            String costo,
            String precio,
            int stock
    ) {
        Producto producto = new Producto();
        producto.setCodigo(codigo);
        producto.setNombre(nombre);
        producto.setCategoria(categoria);
        producto.setCapacidadMl(new BigDecimal("1000"));
        producto.setUnidadesPorPaca(unidadesPorPaca);
        producto.setCostoUnitario(new BigDecimal(costo));
        producto.setPrecioVenta(new BigDecimal(precio));
        producto.setStockDisponible(stock);
        producto.setStockMinimo(10);
        producto.setUnidadMedida("unidad");
        producto.setActivo(true);
        return productoRepository.saveAndFlush(producto);
    }

    private ItemCompraRequest itemCompra(
            Long idProducto,
            int pacas,
            int unidadesPorPaca,
            String costo
    ) {
        ItemCompraRequest item = new ItemCompraRequest();
        item.setIdProducto(idProducto);
        item.setCantidadPacas(pacas);
        item.setUnidadesPorPaca(unidadesPorPaca);
        item.setCantidad(pacas * unidadesPorPaca);
        item.setCostoUnitario(new BigDecimal(costo));
        return item;
    }

    private VentaRequest venta(
            Long idProducto,
            int cantidad,
            String precio,
            String metodo,
            BigDecimal pagoInicial,
            LocalDate vencimiento
    ) {
        VentaRequest request = new VentaRequest();
        request.setIdCliente("credito".equals(metodo) ? cliente.getId() : null);
        request.setIdUsuario(usuario.getId());
        request.setFechaVenta(LocalDateTime.now().minusMinutes(2));
        request.setMetodoPago(metodo);
        request.setDescuento(BigDecimal.ZERO);
        request.setDetalles(List.of(new ItemVentaRequest(
                idProducto, cantidad, new BigDecimal(precio)
        )));
        if ("credito".equals(metodo)) {
            request.setPagoInicial(pagoInicial);
            request.setFormaPagoInicial("efectivo");
            request.setFechaVencimientoCredito(vencimiento);
            request.setObservacionesCredito("Crédito ficticio de integración");
        }
        return request;
    }

    private AbonoCreditoRequest abono(String monto, String clave) {
        AbonoCreditoRequest request = new AbonoCreditoRequest();
        request.setMonto(new BigDecimal(monto));
        request.setFormaPago("transferencia");
        request.setFechaPago(LocalDateTime.now().minusMinutes(1));
        request.setObservaciones("Abono ficticio de integración");
        request.setIdUsuario(usuario.getId());
        request.setClaveIdempotencia(clave);
        return request;
    }
}
