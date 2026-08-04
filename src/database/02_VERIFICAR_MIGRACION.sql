-- Verificación posterior. No modifica datos.
SELECT table_name
FROM information_schema.tables
WHERE table_schema='public'
  AND table_name IN (
    'proveedores','compras','detalle_compras','facturas_proveedores',
    'pagos_proveedores','auditoria_compras','auditoria_pagos_proveedores'
  )
ORDER BY table_name;

SELECT routine_name
FROM information_schema.routines
WHERE routine_schema='public' AND routine_name='fn_generar_numero_compra';
