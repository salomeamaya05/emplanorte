# EMPLANORTE 2.0 — Módulos financieros implementados

## Alcance

Esta versión agrega al sistema existente, sin reemplazar ventas, inventario, gastos, clientes ni cotizaciones:

- Proveedores.
- Compras de inventario.
- Costo promedio ponderado por producto.
- Facturas recibidas de proveedores.
- Pagos y abonos a proveedores.
- Soportes PDF/imagen en un bucket privado de Supabase Storage.
- Anulación auditada de compras y pagos.
- Dashboard financiero integral.

## Navegación nueva

- **Compras:** inversión en mercancía, productos comprados, flete, impuestos y descuento.
- **Facturas y pagos:** cuentas por pagar, vencimientos, abonos, soportes y estados.
- **Proveedores:** datos de contacto, estado e indicadores acumulados.

## Reglas financieras

- Las compras de mercancía no se registran como gastos operativos.
- Una compra aumenta el stock y recalcula el costo promedio ponderado.
- Las ventas conservan el costo histórico capturado en `detalle_ventas`.
- Las ventas anuladas no suman en las métricas financieras.
- Las compras anuladas no suman y solo se revierten cuando el inventario sigue exactamente como quedó después de la compra.
- Los pagos anulados restauran el saldo de la factura.

## Indicadores del dashboard

- Ventas netas.
- Número de ventas completadas.
- Compras del periodo.
- Costo de productos vendidos.
- Ganancia bruta.
- Gastos operativos.
- Ganancia neta.
- Inventario valorizado al costo.
- Cuentas por pagar.
- Capital invertido acumulado.
- Capital recuperado mediante costo de ventas.
- Facturas vencidas y próximas a vencer.
- Productos con stock bajo.
- Evolución mensual y principales proveedores.

## Base de datos

Los scripts nuevos están en `src/database` y deben ejecutarse en este orden:

1. `00_PREVALIDAR_SUPABASE.sql`
2. `01_MIGRACION_PROVEEDORES_COMPRAS_FACTURAS.sql`
3. `02_VERIFICAR_MIGRACION.sql`
4. `03_CONFIGURAR_STORAGE_FACTURAS.sql`

**No ejecute `emplanorte_db.sql` sobre la base de datos actual.** Ese archivo es histórico y no es la migración de esta entrega.
