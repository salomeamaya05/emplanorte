package com.emplanorte.service;

import com.emplanorte.dto.DashboardFinancieroResponse;
import com.emplanorte.dto.DashboardResponse;
import com.emplanorte.repository.GastoRepository;
import com.emplanorte.repository.VentaRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class DashboardService {

    private static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/Bogota");
    private static final Locale LOCALE_CO = Locale.forLanguageTag("es-CO");

    private final VentaRepository ventaRepository;
    private final GastoRepository gastoRepository;
    private final JdbcTemplate jdbc;

    public DashboardService(
            VentaRepository ventaRepository,
            GastoRepository gastoRepository,
            JdbcTemplate jdbc
    ) {
        this.ventaRepository = ventaRepository;
        this.gastoRepository = gastoRepository;
        this.jdbc = jdbc;
    }

    public DashboardResponse obtenerResumenFinanciero(LocalDate desde, LocalDate hasta) {
        if (desde == null) desde = LocalDate.now(ZONA_NEGOCIO);
        if (hasta == null) hasta = LocalDate.now(ZONA_NEGOCIO);
        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime fin = hasta.atTime(LocalTime.MAX);
        BigDecimal ventas = ventaRepository.obtenerTotalVentasPorRango(inicio, fin, "completada");
        BigDecimal ganancias = ventaRepository.obtenerGananciaVentasPorRango(inicio, fin, "completada");
        BigDecimal gastos = gastoRepository.obtenerTotalGastosPorRango(desde, hasta);
        return new DashboardResponse(ventas, gastos, ganancias, ganancias.subtract(gastos));
    }

    public DashboardFinancieroResponse obtenerBalanceCompleto(LocalDate desde, LocalDate hasta) {
        LocalDate hoy = LocalDate.now(ZONA_NEGOCIO);
        if (desde == null) desde = hoy.withDayOfMonth(1);
        if (hasta == null) hasta = hoy;
        validarRango(desde, hasta);

        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime fin = hasta.atTime(LocalTime.MAX);
        Map<String, Object> indicadores = jdbc.queryForMap("""
                SELECT
                 COALESCE((SELECT SUM(total) FROM ventas WHERE estado='completada' AND fecha_venta BETWEEN ? AND ?),0) ventas,
                 COALESCE((SELECT COUNT(*) FROM ventas WHERE estado='completada' AND fecha_venta BETWEEN ? AND ?),0) num_ventas,
                 COALESCE((SELECT SUM(total_costo) FROM ventas WHERE estado='completada' AND fecha_venta BETWEEN ? AND ?),0) costo_ventas,
                 COALESCE((SELECT SUM(valor) FROM gastos WHERE fecha_gasto BETWEEN ? AND ?),0) gastos,
                 COALESCE((SELECT SUM(total) FROM compras WHERE estado='registrada' AND fecha_compra BETWEEN ? AND ?),0) compras,
                 COALESCE((SELECT SUM(stock_disponible*costo_unitario) FROM productos WHERE activo=TRUE),0) inventario,
                 COALESCE((SELECT SUM(saldo_pendiente) FROM facturas_proveedores WHERE estado_pago IN ('pendiente','parcial')),0) por_pagar,
                 COALESCE((SELECT SUM(total) FROM compras WHERE estado='registrada'),0) invertido,
                 COALESCE((SELECT SUM(total_costo) FROM ventas WHERE estado='completada'),0) recuperado,
                 COALESCE((SELECT COUNT(*) FROM facturas_proveedores WHERE estado_pago IN ('pendiente','parcial') AND fecha_vencimiento < ?),0) vencidas,
                 COALESCE((SELECT COUNT(*) FROM facturas_proveedores WHERE estado_pago IN ('pendiente','parcial') AND fecha_vencimiento BETWEEN ? AND ?),0) por_vencer,
                 COALESCE((SELECT COUNT(*) FROM productos WHERE activo=TRUE AND stock_disponible<=stock_minimo),0) stock_bajo,
                 COALESCE((SELECT SUM(saldo_pendiente) FROM creditos_venta WHERE estado='pendiente'),0) por_cobrar,
                 COALESCE((SELECT SUM(saldo_pendiente) FROM creditos_venta WHERE estado='pendiente' AND fecha_vencimiento < ?),0) cartera_vencida
                """, inicio, fin, inicio, fin, inicio, fin, desde, hasta, inicio, fin,
                hoy, hoy, hoy.plusDays(7), hoy);

        BigDecimal ventas = decimal(indicadores.get("ventas"));
        BigDecimal costo = decimal(indicadores.get("costo_ventas"));
        BigDecimal gastos = decimal(indicadores.get("gastos"));

        DashboardFinancieroResponse respuesta = new DashboardFinancieroResponse();
        respuesta.setDesde(desde);
        respuesta.setHasta(hasta);
        respuesta.setVentasNetas(ventas);
        respuesta.setRecaudoVentasPeriodo(calcularRecaudo(inicio, fin));
        respuesta.setNumeroVentas(numero(indicadores.get("num_ventas")));
        respuesta.setComprasPeriodo(decimal(indicadores.get("compras")));
        respuesta.setGastosOperativos(gastos);
        respuesta.setCostoVentas(costo);
        respuesta.setGananciaBruta(ventas.subtract(costo));
        respuesta.setGananciaNeta(ventas.subtract(costo).subtract(gastos));
        respuesta.setInventarioValorizado(decimal(indicadores.get("inventario")));
        respuesta.setCuentasPorPagar(decimal(indicadores.get("por_pagar")));
        respuesta.setCuentasPorCobrar(decimal(indicadores.get("por_cobrar")));
        respuesta.setCarteraVencida(decimal(indicadores.get("cartera_vencida")));
        respuesta.setCapitalInvertidoAcumulado(decimal(indicadores.get("invertido")));
        respuesta.setCapitalRecuperadoAcumulado(decimal(indicadores.get("recuperado")));
        respuesta.setFacturasVencidas(numero(indicadores.get("vencidas")));
        respuesta.setFacturasPorVencer(numero(indicadores.get("por_vencer")));
        respuesta.setProductosStockBajo(numero(indicadores.get("stock_bajo")));
        respuesta.setSerieMensual(construirSerie(desde, hasta));
        respuesta.setProveedoresTop(proveedoresTop(desde, hasta));
        respuesta.setAlertasFacturas(alertasFacturas(hoy));
        return respuesta;
    }

    /**
     * Separa la venta contable del dinero realmente recibido: las ventas de
     * contado se recaudan al vender y los créditos solo cuando se registra un abono.
     */
    private BigDecimal calcularRecaudo(LocalDateTime inicio, LocalDateTime fin) {
        Map<String, Object> resultado = jdbc.queryForMap("""
                SELECT
                 COALESCE((SELECT SUM(total) FROM ventas
                           WHERE estado='completada' AND metodo_pago<>'credito'
                           AND fecha_venta BETWEEN ? AND ?),0)
                 +
                 COALESCE((SELECT SUM(a.monto) FROM abonos_credito a
                           JOIN creditos_venta c ON c.id=a.id_credito
                           JOIN ventas v ON v.id=c.id_venta
                           WHERE v.estado='completada' AND c.estado<>'anulado'
                           AND a.fecha_pago BETWEEN ? AND ?),0) recaudo
                """, inicio, fin, inicio, fin);
        return decimal(resultado.get("recaudo"));
    }

    /**
     * La agrupación se completa en Java para que el dashboard funcione igual
     * en PostgreSQL (producción) y H2 (localhost), sin generate_series/date_trunc.
     */
    private List<DashboardFinancieroResponse.SerieMensual> construirSerie(
            LocalDate desde,
            LocalDate hasta
    ) {
        boolean porDia = ChronoUnit.DAYS.between(desde, hasta) <= 45;
        Map<String, AcumuladoPeriodo> periodos = inicializarPeriodos(desde, hasta, porDia);
        LocalDateTime inicio = desde.atStartOfDay();
        LocalDateTime fin = hasta.atTime(LocalTime.MAX);

        List<VentaPeriodo> ventas = jdbc.query("""
                SELECT fecha_venta,total,total_costo,metodo_pago
                FROM ventas
                WHERE estado='completada' AND fecha_venta BETWEEN ? AND ?
                """, (rs, fila) -> new VentaPeriodo(
                rs.getTimestamp("fecha_venta").toLocalDateTime(),
                rs.getBigDecimal("total"),
                rs.getBigDecimal("total_costo"),
                rs.getString("metodo_pago")
        ), inicio, fin);
        for (VentaPeriodo venta : ventas) {
            AcumuladoPeriodo periodo = periodos.get(clave(venta.fecha().toLocalDate(), porDia));
            if (periodo == null) continue;
            periodo.ventas = periodo.ventas.add(valorSeguro(venta.total()));
            periodo.costo = periodo.costo.add(valorSeguro(venta.costo()));
            if (!"credito".equalsIgnoreCase(venta.metodoPago())) {
                periodo.recaudo = periodo.recaudo.add(valorSeguro(venta.total()));
            }
        }

        List<MovimientoPeriodo> abonos = jdbc.query("""
                SELECT a.fecha_pago,a.monto
                FROM abonos_credito a
                JOIN creditos_venta c ON c.id=a.id_credito
                JOIN ventas v ON v.id=c.id_venta
                WHERE v.estado='completada' AND c.estado<>'anulado'
                  AND a.fecha_pago BETWEEN ? AND ?
                """, (rs, fila) -> new MovimientoPeriodo(
                rs.getTimestamp("fecha_pago").toLocalDateTime().toLocalDate(),
                rs.getBigDecimal("monto")
        ), inicio, fin);
        for (MovimientoPeriodo abono : abonos) {
            AcumuladoPeriodo periodo = periodos.get(clave(abono.fecha(), porDia));
            if (periodo != null) periodo.recaudo = periodo.recaudo.add(valorSeguro(abono.valor()));
        }

        List<MovimientoPeriodo> compras = jdbc.query("""
                SELECT fecha_compra,total FROM compras
                WHERE estado='registrada' AND fecha_compra BETWEEN ? AND ?
                """, (rs, fila) -> new MovimientoPeriodo(
                rs.getTimestamp("fecha_compra").toLocalDateTime().toLocalDate(),
                rs.getBigDecimal("total")
        ), inicio, fin);
        for (MovimientoPeriodo compra : compras) {
            AcumuladoPeriodo periodo = periodos.get(clave(compra.fecha(), porDia));
            if (periodo != null) periodo.compras = periodo.compras.add(valorSeguro(compra.valor()));
        }

        List<MovimientoPeriodo> movimientosGastos = jdbc.query("""
                SELECT fecha_gasto,valor FROM gastos
                WHERE fecha_gasto BETWEEN ? AND ?
                """, (rs, fila) -> new MovimientoPeriodo(
                rs.getDate("fecha_gasto").toLocalDate(),
                rs.getBigDecimal("valor")
        ), desde, hasta);
        for (MovimientoPeriodo gasto : movimientosGastos) {
            AcumuladoPeriodo periodo = periodos.get(clave(gasto.fecha(), porDia));
            if (periodo != null) periodo.gastos = periodo.gastos.add(valorSeguro(gasto.valor()));
        }

        List<DashboardFinancieroResponse.SerieMensual> serie = new ArrayList<>();
        for (Map.Entry<String, AcumuladoPeriodo> entrada : periodos.entrySet()) {
            AcumuladoPeriodo periodo = entrada.getValue();
            serie.add(new DashboardFinancieroResponse.SerieMensual(
                    entrada.getKey(),
                    periodo.etiqueta,
                    periodo.ventas,
                    periodo.recaudo,
                    periodo.compras,
                    periodo.gastos,
                    periodo.ventas.subtract(periodo.costo).subtract(periodo.gastos)
            ));
        }
        return serie;
    }

    private Map<String, AcumuladoPeriodo> inicializarPeriodos(
            LocalDate desde,
            LocalDate hasta,
            boolean porDia
    ) {
        Map<String, AcumuladoPeriodo> periodos = new LinkedHashMap<>();
        LocalDate cursor = porDia ? desde : desde.withDayOfMonth(1);
        LocalDate limite = porDia ? hasta : hasta.withDayOfMonth(1);
        while (!cursor.isAfter(limite)) {
            String etiqueta = porDia
                    ? cursor.getDayOfMonth() + " "
                    + cursor.getMonth().getDisplayName(TextStyle.SHORT, LOCALE_CO)
                    : cursor.getMonth().getDisplayName(TextStyle.SHORT, LOCALE_CO)
                    + " " + cursor.getYear();
            periodos.put(clave(cursor, porDia), new AcumuladoPeriodo(etiqueta));
            cursor = porDia ? cursor.plusDays(1) : cursor.plusMonths(1);
        }
        return periodos;
    }

    private String clave(LocalDate fecha, boolean porDia) {
        return porDia ? fecha.toString() : YearMonth.from(fecha).toString();
    }

    private List<DashboardFinancieroResponse.ProveedorTop> proveedoresTop(
            LocalDate desde,
            LocalDate hasta
    ) {
        return jdbc.query("""
                SELECT p.id,p.razon_social,SUM(c.total) total
                FROM compras c JOIN proveedores p ON p.id=c.id_proveedor
                WHERE c.estado='registrada' AND c.fecha_compra BETWEEN ? AND ?
                GROUP BY p.id,p.razon_social ORDER BY total DESC LIMIT 5
                """, (rs, fila) -> new DashboardFinancieroResponse.ProveedorTop(
                rs.getLong(1), rs.getString(2), rs.getBigDecimal(3)
        ), desde.atStartOfDay(), hasta.atTime(LocalTime.MAX));
    }

    private List<DashboardFinancieroResponse.AlertaFactura> alertasFacturas(LocalDate hoy) {
        return jdbc.query("""
                SELECT f.id,f.numero_factura,p.razon_social,f.fecha_vencimiento,f.saldo_pendiente
                FROM facturas_proveedores f
                JOIN proveedores p ON p.id=f.id_proveedor
                WHERE f.estado_pago IN ('pendiente','parcial')
                  AND f.fecha_vencimiento IS NOT NULL
                  AND f.fecha_vencimiento <= ?
                ORDER BY f.fecha_vencimiento LIMIT 10
                """, (rs, fila) -> {
            LocalDate vencimiento = rs.getDate(4).toLocalDate();
            long dias = ChronoUnit.DAYS.between(hoy, vencimiento);
            return new DashboardFinancieroResponse.AlertaFactura(
                    rs.getLong(1),
                    rs.getString(2),
                    rs.getString(3),
                    vencimiento,
                    rs.getBigDecimal(5),
                    dias < 0 ? "vencida" : "por_vencer",
                    dias
            );
        }, hoy.plusDays(7));
    }

    private void validarRango(LocalDate desde, LocalDate hasta) {
        if (desde.isAfter(hasta)) {
            throw new RuntimeException("La fecha inicial no puede ser posterior a la final");
        }
    }

    private BigDecimal decimal(Object valor) {
        return valor == null ? BigDecimal.ZERO : new BigDecimal(valor.toString());
    }

    private BigDecimal valorSeguro(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO : valor;
    }

    private Long numero(Object valor) {
        return valor == null ? 0L : Long.valueOf(valor.toString());
    }

    private record VentaPeriodo(
            LocalDateTime fecha,
            BigDecimal total,
            BigDecimal costo,
            String metodoPago
    ) {}

    private record MovimientoPeriodo(LocalDate fecha, BigDecimal valor) {}

    private static final class AcumuladoPeriodo {
        private final String etiqueta;
        private BigDecimal ventas = BigDecimal.ZERO;
        private BigDecimal recaudo = BigDecimal.ZERO;
        private BigDecimal costo = BigDecimal.ZERO;
        private BigDecimal compras = BigDecimal.ZERO;
        private BigDecimal gastos = BigDecimal.ZERO;

        private AcumuladoPeriodo(String etiqueta) {
            this.etiqueta = etiqueta;
        }
    }
}
