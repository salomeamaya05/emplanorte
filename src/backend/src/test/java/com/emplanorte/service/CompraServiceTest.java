package com.emplanorte.service;

import com.emplanorte.dto.CompraRequest;
import com.emplanorte.dto.ItemCompraRequest;
import com.emplanorte.model.*;
import com.emplanorte.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CompraService — distribución del flete por pacas")
class CompraServiceTest {

    @Mock private CompraRepository compraRepo;
    @Mock private DetalleCompraRepository detalleRepo;
    @Mock private ProveedorRepository proveedorRepo;
    @Mock private UsuarioRepository usuarioRepo;
    @Mock private ProductoRepository productoRepo;
    @Mock private FacturaProveedorRepository facturaRepo;
    @Mock private PagoProveedorRepository pagoRepo;
    @Mock private AuditoriaCompraRepository auditoriaRepo;
    @Mock private PasswordEncoder encoder;
    @Mock private FacturaProveedorService facturaService;

    private CompraService service;
    private Proveedor proveedor;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        service = new CompraService(
                compraRepo, detalleRepo, proveedorRepo, usuarioRepo, productoRepo,
                facturaRepo, pagoRepo, auditoriaRepo, encoder, facturaService
        );
        proveedor = new Proveedor();
        proveedor.setId(1L);
        proveedor.setActivo(true);
        usuario = new Usuario();
        usuario.setId(2L);

        when(proveedorRepo.findById(1L)).thenReturn(Optional.of(proveedor));
        when(usuarioRepo.findById(2L)).thenReturn(Optional.of(usuario));
        lenient().when(compraRepo.generarNumeroCompra()).thenReturn("CMP-000001");
        lenient().when(compraRepo.save(any(Compra.class))).thenAnswer(invocation -> {
            Compra compra = invocation.getArgument(0);
            compra.setId(10L);
            return compra;
        });
        lenient().when(productoRepo.save(any(Producto.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(detalleRepo.save(any(DetalleCompra.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(auditoriaRepo.save(any(AuditoriaCompra.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    @DisplayName("Una paca de 32 y una de 150 reciben el mismo flete por paca, no por envase")
    void registrar_distribuyeFletePorPacasYCalculaCostoUnitario() {
        Producto grande = producto(11L, "Envase 5 litros", "2450");
        Producto pequeno = producto(12L, "Envase 150 ml", "500");
        when(productoRepo.buscarPorIdParaActualizar(11L)).thenReturn(Optional.of(grande));
        when(productoRepo.buscarPorIdParaActualizar(12L)).thenReturn(Optional.of(pequeno));

        Compra resultado = service.registrar(compra(
                item(11L, 10, 32, "2450"),
                item(12L, 15, 150, "500")
        ));

        ArgumentCaptor<DetalleCompra> captor = ArgumentCaptor.forClass(DetalleCompra.class);
        verify(detalleRepo, times(2)).save(captor.capture());
        List<DetalleCompra> detalles = captor.getAllValues();
        DetalleCompra detalleGrande = detalles.get(0);
        DetalleCompra detallePequeno = detalles.get(1);

        assertThat(resultado.getMetodoDistribucionFlete()).isEqualTo("pacas");
        assertThat(resultado.getFlete()).isEqualByComparingTo("100000.00");
        assertThat(detalleGrande.getCantidad()).isEqualTo(320);
        assertThat(detallePequeno.getCantidad()).isEqualTo(2250);
        assertThat(detalleGrande.getFleteAsignado()).isEqualByComparingTo("40000.00");
        assertThat(detallePequeno.getFleteAsignado()).isEqualByComparingTo("60000.00");
        assertThat(detalleGrande.getFleteUnitario()).isEqualByComparingTo("125.0000");
        assertThat(detallePequeno.getFleteUnitario()).isEqualByComparingTo("26.6667");
        assertThat(detalleGrande.getCostoUnitarioInventario()).isEqualByComparingTo("2575.00");
        assertThat(detallePequeno.getCostoUnitarioInventario()).isEqualByComparingTo("526.67");
        assertThat(detalleGrande.getFleteAsignado().add(detallePequeno.getFleteAsignado()))
                .isEqualByComparingTo(resultado.getFlete());
    }

    @Test
    @DisplayName("El redondeo conserva exactamente el total del flete")
    void registrar_conDivisionNoExacta_noPierdeCentavosDeFlete() {
        Producto primero = producto(11L, "Producto A", "100");
        Producto segundo = producto(12L, "Producto B", "100");
        when(productoRepo.buscarPorIdParaActualizar(11L)).thenReturn(Optional.of(primero));
        when(productoRepo.buscarPorIdParaActualizar(12L)).thenReturn(Optional.of(segundo));

        CompraRequest request = compra(
                item(11L, 1, 10, "100"),
                item(12L, 2, 10, "100")
        );
        request.setFlete(new BigDecimal("100.00"));
        service.registrar(request);

        ArgumentCaptor<DetalleCompra> captor = ArgumentCaptor.forClass(DetalleCompra.class);
        verify(detalleRepo, times(2)).save(captor.capture());
        List<DetalleCompra> detalles = captor.getAllValues();

        assertThat(detalles.get(0).getFleteAsignado()).isEqualByComparingTo("33.33");
        assertThat(detalles.get(1).getFleteAsignado()).isEqualByComparingTo("66.67");
        assertThat(detalles.stream().map(DetalleCompra::getFleteAsignado).reduce(BigDecimal.ZERO, BigDecimal::add))
                .isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Rechaza una cantidad que no coincide con pacas por unidades")
    void registrar_cantidadInconsistente_rechazaCompra() {
        ItemCompraRequest item = item(11L, 2, 32, "100");
        item.setCantidad(63);

        assertThatThrownBy(() -> service.registrar(compra(item)))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("pacas × unidades por paca");
        verifyNoInteractions(productoRepo);
    }

    @Test
    @DisplayName("Actualiza el costo promedio ponderado usando el inventario anterior")
    void registrar_conStockAnterior_calculaCostoPromedioPonderado() {
        Producto producto = producto(11L, "Envase 5 litros", "2000");
        producto.setStockDisponible(100);
        when(productoRepo.buscarPorIdParaActualizar(11L)).thenReturn(Optional.of(producto));
        CompraRequest request = compra(item(11L, 10, 32, "2450"));
        request.setFlete(new BigDecimal("40000"));

        service.registrar(request);

        ArgumentCaptor<DetalleCompra> captor = ArgumentCaptor.forClass(DetalleCompra.class);
        verify(detalleRepo).save(captor.capture());
        DetalleCompra detalle = captor.getValue();
        assertThat(detalle.getStockAnterior()).isEqualTo(100);
        assertThat(detalle.getStockPosterior()).isEqualTo(420);
        assertThat(detalle.getCostoUnitarioInventario()).isEqualByComparingTo("2575.00");
        assertThat(detalle.getCostoPromedioPosterior()).isEqualByComparingTo("2438.10");
        assertThat(producto.getStockDisponible()).isEqualTo(420);
        assertThat(producto.getCostoUnitario()).isEqualByComparingTo("2438.10");
    }

    @Test
    @DisplayName("Distribuye el último centavo sin crear ni perder dinero entre tres líneas")
    void registrar_tresLineas_conservaCentavosExactos() {
        for (long id = 11; id <= 13; id++) {
            when(productoRepo.buscarPorIdParaActualizar(id))
                    .thenReturn(Optional.of(producto(id, "Producto " + id, "100")));
        }
        CompraRequest request = compra(
                item(11L, 1, 10, "100"),
                item(12L, 1, 10, "100"),
                item(13L, 1, 10, "100")
        );
        request.setFlete(new BigDecimal("100.00"));

        service.registrar(request);

        ArgumentCaptor<DetalleCompra> captor = ArgumentCaptor.forClass(DetalleCompra.class);
        verify(detalleRepo, times(3)).save(captor.capture());
        List<DetalleCompra> detalles = captor.getAllValues();
        assertThat(detalles).extracting(DetalleCompra::getFleteAsignado)
                .containsExactly(new BigDecimal("33.33"), new BigDecimal("33.33"), new BigDecimal("33.34"));
        assertThat(detalles.stream().map(DetalleCompra::getFleteAsignado)
                .reduce(BigDecimal.ZERO, BigDecimal::add)).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("Impuestos y descuento forman parte del costo de inventario sin cambiar el reparto del flete")
    void registrar_conImpuestosYDescuento_calculaCostoReal() {
        Producto producto = producto(11L, "Producto A", "100");
        when(productoRepo.buscarPorIdParaActualizar(11L)).thenReturn(Optional.of(producto));
        CompraRequest request = compra(item(11L, 1, 10, "100"));
        request.setFlete(new BigDecimal("100"));
        request.setImpuestos(new BigDecimal("50"));
        request.setDescuento(new BigDecimal("20"));

        Compra compra = service.registrar(request);

        ArgumentCaptor<DetalleCompra> captor = ArgumentCaptor.forClass(DetalleCompra.class);
        verify(detalleRepo).save(captor.capture());
        assertThat(compra.getTotal()).isEqualByComparingTo("1130.00");
        assertThat(captor.getValue().getFleteAsignado()).isEqualByComparingTo("100.00");
        assertThat(captor.getValue().getCostoUnitarioInventario()).isEqualByComparingTo("113.00");
    }

    @Test
    @DisplayName("Rechaza pacas o unidades por paca iguales a cero")
    void registrar_empaqueEnCero_rechazaCompra() {
        ItemCompraRequest sinPacas = item(11L, 1, 32, "100");
        sinPacas.setCantidadPacas(0);
        ItemCompraRequest sinUnidades = item(12L, 1, 32, "100");
        sinUnidades.setUnidadesPorPaca(0);

        assertThatThrownBy(() -> service.registrar(compra(sinPacas)))
                .hasMessageContaining("pacas debe ser mayor a cero");
        assertThatThrownBy(() -> service.registrar(compra(sinUnidades)))
                .hasMessageContaining("unidades por paca deben ser mayores a cero");
        verifyNoInteractions(productoRepo);
    }

    @Test
    @DisplayName("Rechaza empaques incompletos y cantidades que exceden el máximo")
    void registrar_empaqueIncompletoOExcesivo_rechazaCompra() {
        ItemCompraRequest incompleto = item(11L, 1, 32, "100");
        incompleto.setUnidadesPorPaca(null);
        ItemCompraRequest excesivo = new ItemCompraRequest();
        excesivo.setIdProducto(12L);
        excesivo.setCantidadPacas(Integer.MAX_VALUE);
        excesivo.setUnidadesPorPaca(2);
        excesivo.setCostoUnitario(new BigDecimal("100"));

        assertThatThrownBy(() -> service.registrar(compra(incompleto)))
                .hasMessageContaining("tanto las pacas como las unidades por paca");
        assertThatThrownBy(() -> service.registrar(compra(excesivo)))
                .hasMessageContaining("cantidad total del producto supera el máximo");
        verifyNoInteractions(productoRepo);
    }

    @Test
    @DisplayName("Rechaza el mismo producto repetido para evitar cálculos ambiguos")
    void registrar_productoDuplicado_rechazaCompra() {
        Producto producto = producto(11L, "Producto A", "100");
        when(productoRepo.buscarPorIdParaActualizar(11L)).thenReturn(Optional.of(producto));

        assertThatThrownBy(() -> service.registrar(compra(
                item(11L, 1, 10, "100"),
                item(11L, 2, 10, "100")
        ))).hasMessageContaining("No repita el mismo producto");
        verify(compraRepo, never()).save(any());
        verify(detalleRepo, never()).save(any());
    }

    @Test
    @DisplayName("Acepta registros históricos que solo informan la cantidad total")
    void registrar_formatoHistorico_conservaCompatibilidad() {
        Producto producto = producto(11L, "Producto histórico", "100");
        when(productoRepo.buscarPorIdParaActualizar(11L)).thenReturn(Optional.of(producto));
        ItemCompraRequest item = new ItemCompraRequest();
        item.setIdProducto(11L);
        item.setCantidad(24);
        item.setCostoUnitario(new BigDecimal("100"));
        CompraRequest request = compra(item);
        request.setFlete(BigDecimal.ZERO);

        service.registrar(request);

        ArgumentCaptor<DetalleCompra> captor = ArgumentCaptor.forClass(DetalleCompra.class);
        verify(detalleRepo).save(captor.capture());
        assertThat(captor.getValue().getCantidad()).isEqualTo(24);
        assertThat(captor.getValue().getCantidadPacas()).isEqualTo(1);
        assertThat(captor.getValue().getUnidadesPorPaca()).isEqualTo(24);
    }

    @Test
    @DisplayName("Evita desbordar el stock máximo y no guarda la compra")
    void registrar_stockResultanteExcesivo_rechazaCompra() {
        Producto producto = producto(11L, "Producto saturado", "100");
        producto.setStockDisponible(Integer.MAX_VALUE - 5);
        when(productoRepo.buscarPorIdParaActualizar(11L)).thenReturn(Optional.of(producto));
        CompraRequest request = compra(item(11L, 1, 10, "100"));

        assertThatThrownBy(() -> service.registrar(request))
                .hasMessageContaining("stock resultante")
                .hasMessageContaining("supera el máximo");
        verify(productoRepo, never()).save(any());
        verify(compraRepo, never()).save(any());
    }

    private CompraRequest compra(ItemCompraRequest... items) {
        CompraRequest request = new CompraRequest();
        request.setIdProveedor(1L);
        request.setIdUsuario(2L);
        request.setFlete(new BigDecimal("100000"));
        request.setImpuestos(BigDecimal.ZERO);
        request.setDescuento(BigDecimal.ZERO);
        request.setDetalles(List.of(items));
        request.setRegistrarFactura(false);
        return request;
    }

    private ItemCompraRequest item(Long productoId, int pacas, int unidadesPorPaca, String costo) {
        ItemCompraRequest item = new ItemCompraRequest();
        item.setIdProducto(productoId);
        item.setCantidadPacas(pacas);
        item.setUnidadesPorPaca(unidadesPorPaca);
        item.setCantidad(pacas * unidadesPorPaca);
        item.setCostoUnitario(new BigDecimal(costo));
        return item;
    }

    private Producto producto(Long id, String nombre, String costoAnterior) {
        Producto producto = new Producto();
        producto.setId(id);
        producto.setNombre(nombre);
        producto.setActivo(true);
        producto.setStockDisponible(0);
        producto.setCostoUnitario(new BigDecimal(costoAnterior));
        producto.setUnidadesPorPaca(1);
        return producto;
    }
}
