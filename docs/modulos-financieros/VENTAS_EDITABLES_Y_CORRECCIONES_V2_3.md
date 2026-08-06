# EMPLANORTE v2.3.0 — Edición y corrección segura de ventas

## Regla principal

Una venta completada no permite cambiar productos, cantidades, precios, descuento, total, costo o ganancia directamente. Esos datos afectan inventario y resultados financieros.

## Opciones implementadas

### Editar

Permite corregir únicamente:

- cliente;
- fecha y hora real de la venta;
- método de pago;
- observaciones.

Exige usuario administrador, contraseña y motivo. La venta conserva el estado `completada`, se marca como `editada` y el historial guarda los valores anteriores y nuevos.

### Anular

Devuelve las unidades al inventario, excluye la venta de los cálculos financieros y exige motivo y contraseña. La venta no se elimina.

### Anular y corregir

Anula la venta original, devuelve el inventario y abre una venta nueva precargada. La nueva venta puede corregir productos, cantidades, precios, descuento, cliente y fecha. Ambas ventas quedan enlazadas.

## Fechas

- `fecha_venta`: fecha real elegida por el usuario; se usa en tabla, Dashboard y reportes.
- `creado_en`: fecha de registro del sistema; no se edita.
- La aplicación usa la zona `America/Bogota` para nuevos registros y auditorías.

## Base de datos

Antes de desplegar el backend v2.3.0 se debe ejecutar:

1. `src/database/04_MIGRACION_EDICION_Y_CORRECCION_VENTAS.sql`
2. `src/database/05_VERIFICAR_EDICION_Y_CORRECCION_VENTAS.sql`

La migración es aditiva y no borra ventas, detalles, inventario ni auditorías.
