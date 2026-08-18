package com.emplanorte.service;

import com.emplanorte.dto.*;
import com.emplanorte.repository.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.sql.Date;
import java.time.*;
import java.time.format.TextStyle;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class DashboardService {
    private final VentaRepository ventaRepository; private final GastoRepository gastoRepository; private final JdbcTemplate jdbc;
    public DashboardService(VentaRepository v,GastoRepository g,JdbcTemplate j){ventaRepository=v;gastoRepository=g;jdbc=j;}

    public DashboardResponse obtenerResumenFinanciero(LocalDate desde,LocalDate hasta){
        if(desde==null)desde=LocalDate.now();if(hasta==null)hasta=LocalDate.now();
        LocalDateTime ini=desde.atStartOfDay(),fin=hasta.atTime(LocalTime.MAX);
        BigDecimal ventas=ventaRepository.obtenerTotalVentasPorRango(ini,fin,"completada");
        BigDecimal ganancias=ventaRepository.obtenerGananciaVentasPorRango(ini,fin,"completada");
        BigDecimal gastos=gastoRepository.obtenerTotalGastosPorRango(desde,hasta);
        return new DashboardResponse(ventas,gastos,ganancias,ganancias.subtract(gastos));
    }

    public DashboardFinancieroResponse obtenerBalanceCompleto(LocalDate desde,LocalDate hasta){
        if(desde==null)desde=LocalDate.now().withDayOfMonth(1);if(hasta==null)hasta=LocalDate.now();validarRango(desde,hasta);
        LocalDateTime ini=desde.atStartOfDay(),fin=hasta.atTime(LocalTime.MAX);
        Map<String,Object> k=jdbc.queryForMap("""
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
           COALESCE((SELECT COUNT(*) FROM facturas_proveedores WHERE estado_pago IN ('pendiente','parcial') AND fecha_vencimiento<CURRENT_DATE),0) vencidas,
           COALESCE((SELECT COUNT(*) FROM facturas_proveedores WHERE estado_pago IN ('pendiente','parcial') AND fecha_vencimiento BETWEEN CURRENT_DATE AND CURRENT_DATE+7),0) por_vencer,
           COALESCE((SELECT COUNT(*) FROM productos WHERE activo=TRUE AND stock_disponible<=stock_minimo),0) stock_bajo
        """,ini,fin,ini,fin,ini,fin,desde,hasta,ini,fin);
        BigDecimal ventas=bd(k.get("ventas")),costo=bd(k.get("costo_ventas")),gastos=bd(k.get("gastos"));
        DashboardFinancieroResponse r=new DashboardFinancieroResponse();r.setDesde(desde);r.setHasta(hasta);r.setVentasNetas(ventas);r.setNumeroVentas(num(k.get("num_ventas")));
        r.setComprasPeriodo(bd(k.get("compras")));r.setGastosOperativos(gastos);r.setCostoVentas(costo);r.setGananciaBruta(ventas.subtract(costo));r.setGananciaNeta(ventas.subtract(costo).subtract(gastos));
        r.setInventarioValorizado(bd(k.get("inventario")));r.setCuentasPorPagar(bd(k.get("por_pagar")));r.setCapitalInvertidoAcumulado(bd(k.get("invertido")));r.setCapitalRecuperadoAcumulado(bd(k.get("recuperado")));
        r.setFacturasVencidas(num(k.get("vencidas")));r.setFacturasPorVencer(num(k.get("por_vencer")));r.setProductosStockBajo(num(k.get("stock_bajo")));
        r.setSerieMensual(serie(desde,hasta));r.setProveedoresTop(top(desde,hasta));r.setAlertasFacturas(alertas());return r;
    }

    private List<DashboardFinancieroResponse.SerieMensual> serie(LocalDate desde, LocalDate hasta) {
        long dias = ChronoUnit.DAYS.between(desde, hasta);
        if (dias <= 45) {
            return jdbc.query("""
              WITH periodos AS (
                SELECT generate_series(?::date, ?::date, interval '1 day')::date periodo
              ),
              v AS (
                SELECT fecha_venta::date periodo, SUM(total) ventas, SUM(total_costo) costo
                FROM ventas
                WHERE estado='completada' AND fecha_venta BETWEEN ? AND ?
                GROUP BY 1
              ),
              c AS (
                SELECT fecha_compra::date periodo, SUM(total) compras
                FROM compras
                WHERE estado='registrada' AND fecha_compra BETWEEN ? AND ?
                GROUP BY 1
              ),
              g AS (
                SELECT fecha_gasto::date periodo, SUM(valor) gastos
                FROM gastos
                WHERE fecha_gasto BETWEEN ? AND ?
                GROUP BY 1
              )
              SELECT p.periodo,
                     COALESCE(v.ventas,0),
                     COALESCE(c.compras,0),
                     COALESCE(g.gastos,0),
                     COALESCE(v.ventas,0)-COALESCE(v.costo,0)-COALESCE(g.gastos,0)
              FROM periodos p
              LEFT JOIN v USING(periodo)
              LEFT JOIN c USING(periodo)
              LEFT JOIN g USING(periodo)
              ORDER BY p.periodo
            """, (rs, i) -> {
                LocalDate periodo = rs.getDate(1).toLocalDate();
                String etiqueta = periodo.getDayOfMonth() + " " + periodo.getMonth().getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("es-CO"));
                return new DashboardFinancieroResponse.SerieMensual(periodo.toString(), etiqueta,
                        rs.getBigDecimal(2), rs.getBigDecimal(3), rs.getBigDecimal(4), rs.getBigDecimal(5));
            }, Date.valueOf(desde), Date.valueOf(hasta),
               desde.atStartOfDay(), hasta.atTime(LocalTime.MAX),
               desde.atStartOfDay(), hasta.atTime(LocalTime.MAX),
               Date.valueOf(desde), Date.valueOf(hasta));
        }

        LocalDate primer = desde.withDayOfMonth(1);
        LocalDate ultimo = hasta.withDayOfMonth(1);
        return jdbc.query("""
          WITH periodos AS (
            SELECT generate_series(?::date, ?::date, interval '1 month')::date periodo
          ),
          v AS (
            SELECT date_trunc('month',fecha_venta)::date periodo, SUM(total) ventas, SUM(total_costo) costo
            FROM ventas
            WHERE estado='completada' AND fecha_venta BETWEEN ? AND ?
            GROUP BY 1
          ),
          c AS (
            SELECT date_trunc('month',fecha_compra)::date periodo, SUM(total) compras
            FROM compras
            WHERE estado='registrada' AND fecha_compra BETWEEN ? AND ?
            GROUP BY 1
          ),
          g AS (
            SELECT date_trunc('month',fecha_gasto)::date periodo, SUM(valor) gastos
            FROM gastos
            WHERE fecha_gasto BETWEEN ? AND ?
            GROUP BY 1
          )
          SELECT p.periodo,
                 COALESCE(v.ventas,0),
                 COALESCE(c.compras,0),
                 COALESCE(g.gastos,0),
                 COALESCE(v.ventas,0)-COALESCE(v.costo,0)-COALESCE(g.gastos,0)
          FROM periodos p
          LEFT JOIN v USING(periodo)
          LEFT JOIN c USING(periodo)
          LEFT JOIN g USING(periodo)
          ORDER BY p.periodo
        """, (rs, i) -> {
            LocalDate periodo = rs.getDate(1).toLocalDate();
            String etiqueta = periodo.getMonth().getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("es-CO")) + " " + periodo.getYear();
            return new DashboardFinancieroResponse.SerieMensual(periodo.toString().substring(0, 7), etiqueta,
                    rs.getBigDecimal(2), rs.getBigDecimal(3), rs.getBigDecimal(4), rs.getBigDecimal(5));
        }, Date.valueOf(primer), Date.valueOf(ultimo),
           desde.atStartOfDay(), hasta.atTime(LocalTime.MAX),
           desde.atStartOfDay(), hasta.atTime(LocalTime.MAX),
           Date.valueOf(desde), Date.valueOf(hasta));
    }
    private List<DashboardFinancieroResponse.ProveedorTop> top(LocalDate d,LocalDate h){return jdbc.query("SELECT p.id,p.razon_social,SUM(c.total) total FROM compras c JOIN proveedores p ON p.id=c.id_proveedor WHERE c.estado='registrada' AND c.fecha_compra BETWEEN ? AND ? GROUP BY p.id,p.razon_social ORDER BY total DESC LIMIT 5",(rs,i)->new DashboardFinancieroResponse.ProveedorTop(rs.getLong(1),rs.getString(2),rs.getBigDecimal(3)),d.atStartOfDay(),h.atTime(LocalTime.MAX));}
    private List<DashboardFinancieroResponse.AlertaFactura> alertas(){return jdbc.query("SELECT f.id,f.numero_factura,p.razon_social,f.fecha_vencimiento,f.saldo_pendiente,(f.fecha_vencimiento-CURRENT_DATE) dias FROM facturas_proveedores f JOIN proveedores p ON p.id=f.id_proveedor WHERE f.estado_pago IN ('pendiente','parcial') AND f.fecha_vencimiento IS NOT NULL AND f.fecha_vencimiento<=CURRENT_DATE+7 ORDER BY f.fecha_vencimiento LIMIT 10",(rs,i)->{long dias=rs.getLong(6);return new DashboardFinancieroResponse.AlertaFactura(rs.getLong(1),rs.getString(2),rs.getString(3),rs.getDate(4).toLocalDate(),rs.getBigDecimal(5),dias<0?"vencida":"por_vencer",dias);});}
    private void validarRango(LocalDate d,LocalDate h){if(d.isAfter(h))throw new RuntimeException("La fecha inicial no puede ser posterior a la final");}
    private BigDecimal bd(Object o){return o==null?BigDecimal.ZERO:new BigDecimal(o.toString());}
    private Long num(Object o){return o==null?0L:Long.valueOf(o.toString());}
}
