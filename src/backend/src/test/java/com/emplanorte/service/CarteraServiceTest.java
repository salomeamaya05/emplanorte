package com.emplanorte.service;

import com.emplanorte.dto.AbonoCreditoRequest;
import com.emplanorte.dto.CarteraResumenResponse;
import com.emplanorte.dto.VentaRequest;
import com.emplanorte.model.*;
import com.emplanorte.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CarteraService — créditos y abonos trazables")
class CarteraServiceTest {

    @Mock private CreditoVentaRepository creditoRepository;
    @Mock private AbonoCreditoRepository abonoRepository;
    @Mock private UsuarioRepository usuarioRepository;
    @Mock private ClienteRepository clienteRepository;
    @Mock private VentaRepository ventaRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private CarteraService service;
    private Usuario usuario;
    private Cliente cliente;
    private Venta venta;

    @BeforeEach
    void setUp() {
        service = new CarteraService(
                creditoRepository, abonoRepository, usuarioRepository,
                clienteRepository, ventaRepository, passwordEncoder
        );
        usuario = new Usuario();
        usuario.setId(7L);
        usuario.setNombre("Admin local");
        usuario.setActivo(true);
        usuario.setRol("administrador");

        cliente = new Cliente();
        cliente.setId(8L);
        cliente.setNombre("Cliente local");
        cliente.setTelefono("3000000000");

        venta = new Venta();
        venta.setId(10L);
        venta.setNumeroVenta("VTA-000010");
        venta.setCliente(cliente);
        venta.setUsuario(usuario);
        venta.setFechaVenta(LocalDateTime.now().minusDays(1));
        venta.setMetodoPago("credito");
        venta.setEstado("completada");
        venta.setTotal(new BigDecimal("1000000.00"));

        lenient().when(creditoRepository.save(any(CreditoVenta.class))).thenAnswer(invocation -> {
            CreditoVenta credito = invocation.getArgument(0);
            if (credito.getId() == null) credito.setId(30L);
            return credito;
        });
        lenient().when(abonoRepository.save(any(AbonoCredito.class))).thenAnswer(invocation -> {
            AbonoCredito abono = invocation.getArgument(0);
            if (abono.getId() == null) abono.setId(40L);
            return abono;
        });
    }

    @Test
    @DisplayName("Crea crédito de 1.000.000, registra pago inicial de 300.000 y deja saldo de 700.000")
    void crearCreditoConPagoInicial() {
        VentaRequest request = solicitudCredito("300000.00");
        when(creditoRepository.findByVentaId(10L)).thenReturn(Optional.empty());

        CreditoVenta credito = service.crearParaVenta(venta, request, usuario);

        assertThat(credito.getTotalCredito()).isEqualByComparingTo("1000000.00");
        assertThat(credito.getSaldoPendiente()).isEqualByComparingTo("700000.00");
        assertThat(credito.getEstado()).isEqualTo("pendiente");
        verify(abonoRepository).save(argThat(abono ->
                abono.getMonto().compareTo(new BigDecimal("300000.00")) == 0
                        && "inicial".equals(abono.getTipo())
                        && "INICIAL-VENTA-10".equals(abono.getClaveIdempotencia())));
    }

    @Test
    @DisplayName("Rechaza pago inicial superior al total")
    void rechazaPagoInicialSuperior() {
        VentaRequest request = solicitudCredito("1000000.01");
        when(creditoRepository.findByVentaId(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crearParaVenta(venta, request, usuario))
                .hasMessageContaining("no puede superar");
        verify(creditoRepository, never()).save(any());
    }

    @Test
    @DisplayName("Una venta no crediticia no puede enviar datos de cartera")
    void contadoNoAceptaDatosCredito() {
        venta.setMetodoPago("efectivo");
        VentaRequest request = solicitudCredito("0");

        assertThatThrownBy(() -> service.crearParaVenta(venta, request, usuario))
                .hasMessageContaining("solo corresponden");
    }

    @Test
    @DisplayName("Abono parcial reduce el saldo sin modificar la venta")
    void registrarAbonoParcial() {
        CreditoVenta credito = credito("700000.00");
        AbonoCreditoRequest request = abono("200000.00", "abono-local-0001");
        when(abonoRepository.findByClaveIdempotencia(request.getClaveIdempotencia()))
                .thenReturn(Optional.empty());
        when(creditoRepository.buscarPorIdParaActualizar(30L)).thenReturn(Optional.of(credito));
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));

        service.registrarAbono(30L, request);

        assertThat(credito.getSaldoPendiente()).isEqualByComparingTo("500000.00");
        assertThat(credito.getEstado()).isEqualTo("pendiente");
        assertThat(venta.getTotal()).isEqualByComparingTo("1000000.00");
    }

    @Test
    @DisplayName("Pago final deja saldo cero y estado pagado")
    void pagoFinalCierraCredito() {
        CreditoVenta credito = credito("500000.00");
        AbonoCreditoRequest request = abono("500000.00", "abono-local-final");
        when(abonoRepository.findByClaveIdempotencia(request.getClaveIdempotencia()))
                .thenReturn(Optional.empty());
        when(creditoRepository.buscarPorIdParaActualizar(30L)).thenReturn(Optional.of(credito));
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));

        service.registrarAbono(30L, request);

        assertThat(credito.getSaldoPendiente()).isEqualByComparingTo("0.00");
        assertThat(credito.getEstado()).isEqualTo("pagado");
    }

    @Test
    @DisplayName("No permite abonar más del saldo ni valores negativos")
    void validaMontoDelAbono() {
        CreditoVenta credito = credito("500000.00");
        when(abonoRepository.findByClaveIdempotencia(anyString())).thenReturn(Optional.empty());
        when(creditoRepository.buscarPorIdParaActualizar(30L)).thenReturn(Optional.of(credito));

        assertThatThrownBy(() -> service.registrarAbono(30L, abono("500000.01", "abono-excesivo-01")))
                .hasMessageContaining("no puede superar");
        assertThatThrownBy(() -> service.registrarAbono(30L, abono("-1", "abono-negativo-01")))
                .hasMessageContaining("mayor a cero");
        verify(abonoRepository, never()).save(any());
    }

    @Test
    @DisplayName("El mismo identificador no duplica un abono por doble clic")
    void idempotenciaEvitaDuplicado() {
        CreditoVenta credito = credito("500000.00");
        AbonoCredito existente = new AbonoCredito();
        existente.setId(99L);
        existente.setCredito(credito);
        existente.setUsuario(usuario);
        existente.setMonto(new BigDecimal("100000.00"));
        existente.setFormaPago("efectivo");
        existente.setFechaPago(LocalDateTime.now());
        existente.setTipo("abono");
        existente.setClaveIdempotencia("doble-click-0001");
        when(abonoRepository.findByClaveIdempotencia("doble-click-0001"))
                .thenReturn(Optional.of(existente));

        var respuesta = service.registrarAbono(30L, abono("100000.00", "doble-click-0001"));

        assertThat(respuesta.getId()).isEqualTo(99L);
        verify(creditoRepository, never()).buscarPorIdParaActualizar(anyLong());
        verify(abonoRepository, never()).save(any());
    }

    @Test
    @DisplayName("No permite anular una venta a crédito que ya recibió dinero")
    void noAnulaCreditoConRecaudos() {
        when(creditoRepository.findByVentaId(10L)).thenReturn(Optional.of(credito("700000.00")));

        assertThatThrownBy(() -> service.validarAnulacion(venta))
                .hasMessageContaining("pagos registrados");
    }

    @Test
    @DisplayName("Un crédito sin recaudos se anula conservando su historia y saldo original")
    void anulaCreditoSinRecaudos() {
        CreditoVenta credito = credito("1000000.00");
        when(creditoRepository.findByVentaId(10L)).thenReturn(Optional.of(credito));

        service.validarAnulacion(venta);
        service.anularPorVenta(venta);

        assertThat(credito.getEstado()).isEqualTo("anulado");
        assertThat(credito.getSaldoPendiente()).isEqualByComparingTo("1000000.00");
    }

    @Test
    @DisplayName("Resumen separa cartera vencida, por vencer y clientes")
    void resumenCartera() {
        CreditoVenta vencido = credito("400000.00");
        vencido.setId(31L);
        vencido.setFechaVencimiento(LocalDate.now().minusDays(2));
        CreditoVenta pendiente = credito("300000.00");
        pendiente.setId(32L);
        pendiente.setFechaVencimiento(LocalDate.now().plusDays(5));
        when(creditoRepository.findAllByOrderByFechaVencimientoAscIdDesc())
                .thenReturn(List.of(vencido, pendiente));

        CarteraResumenResponse resumen = service.resumen();

        assertThat(resumen.getTotalPorCobrar()).isEqualByComparingTo("700000.00");
        assertThat(resumen.getTotalVencido()).isEqualByComparingTo("400000.00");
        assertThat(resumen.getCreditosVencidos()).isEqualTo(1L);
        assertThat(resumen.getClientesConSaldo()).isEqualTo(1L);
    }

    private VentaRequest solicitudCredito(String pagoInicial) {
        VentaRequest request = new VentaRequest();
        request.setPagoInicial(new BigDecimal(pagoInicial));
        request.setFormaPagoInicial("efectivo");
        request.setFechaVencimientoCredito(LocalDate.now().plusDays(30));
        request.setObservacionesCredito("Prueba local");
        return request;
    }

    private AbonoCreditoRequest abono(String monto, String clave) {
        AbonoCreditoRequest request = new AbonoCreditoRequest();
        request.setMonto(new BigDecimal(monto));
        request.setFormaPago("efectivo");
        request.setFechaPago(LocalDateTime.now().minusMinutes(1));
        request.setIdUsuario(7L);
        request.setClaveIdempotencia(clave);
        return request;
    }

    private CreditoVenta credito(String saldo) {
        CreditoVenta credito = new CreditoVenta();
        credito.setId(30L);
        credito.setVenta(venta);
        credito.setCliente(cliente);
        credito.setTotalCredito(new BigDecimal("1000000.00"));
        credito.setSaldoPendiente(new BigDecimal(saldo));
        credito.setFechaVencimiento(LocalDate.now().plusDays(30));
        credito.setEstado("pendiente");
        credito.setVersion(0L);
        return credito;
    }
}
