# Checklist de pruebas — módulos financieros

Marque cada punto antes de fusionar con `main`.

## A. Regresión del sistema existente

- [ ] Inicia sesión con el usuario actual.
- [ ] Lista ventas anteriores sin errores.
- [ ] Una venta anulada continúa visible pero no suma en tarjetas.
- [ ] Registra una venta y descuenta inventario.
- [ ] Anula una venta y devuelve inventario.
- [ ] Lista y registra gastos.
- [ ] Lista inventario, clientes y cotizaciones.

## B. Proveedores

- [ ] Crea un proveedor con NIT/documento único.
- [ ] Rechaza NIT/documento duplicado.
- [ ] Edita datos del proveedor.
- [ ] Desactiva el proveedor sin borrarlo físicamente.
- [ ] Muestra total comprado, pagado y saldo pendiente.

## C. Compras e inventario

Use un producto de prueba y anote antes:

```text
Stock anterior:
Costo anterior:
```

- [ ] Registra una compra con una o más líneas.
- [ ] El stock aumenta exactamente en la cantidad comprada.
- [ ] El costo cambia según promedio ponderado.
- [ ] Flete, impuestos y descuento se incluyen en el valor llevado al inventario.
- [ ] No permite productos repetidos en líneas separadas.
- [ ] No permite costo o cantidad en cero.
- [ ] La compra aparece como `registrada`.

## D. Facturas y pagos

- [ ] Crea factura junto con la compra.
- [ ] También permite crearla después desde una compra sin factura.
- [ ] No permite dos facturas para una misma compra.
- [ ] No permite repetir el mismo número para el mismo proveedor.
- [ ] Registra un abono parcial y actualiza saldo/estado a `parcial`.
- [ ] Registra el pago restante y cambia a `pagada`.
- [ ] No permite pagar más que el saldo.
- [ ] Anula un pago con contraseña y restaura el saldo.

## E. Soportes

- [ ] Sube PDF menor o igual a 10 MB.
- [ ] Sube JPG/PNG/WEBP menor o igual a 10 MB.
- [ ] Rechaza formatos no permitidos.
- [ ] Abre el soporte desde la tabla de facturas.
- [ ] El bucket `facturas-proveedores` aparece como privado.

## F. Anulación de compras

- [ ] Una compra sin pagos y sin movimientos posteriores puede anularse.
- [ ] La anulación restaura stock y costo anteriores.
- [ ] La factura vinculada queda `anulada`.
- [ ] Una compra con pagos activos no puede anularse.
- [ ] Una compra cuyo producto cambió después no puede anularse.
- [ ] Se registra auditoría con usuario, fecha y motivo.

## G. Dashboard

- [ ] Ventas netas excluyen anuladas.
- [ ] Compras del periodo excluyen anuladas.
- [ ] Costo de ventas coincide con ventas completadas.
- [ ] Ganancia bruta = ventas netas − costo de ventas.
- [ ] Ganancia neta = ganancia bruta − gastos operativos.
- [ ] Inventario valorizado = stock actual × costo actual.
- [ ] Cuentas por pagar coinciden con saldos pendientes/parciales.
- [ ] Facturas vencidas y próximas a vencer son correctas.
- [ ] La gráfica mensual carga sin errores.

## Resultado final

```text
Fecha de prueba:
Responsable:
Resultado: APROBADO / NO APROBADO
Observaciones:
```
