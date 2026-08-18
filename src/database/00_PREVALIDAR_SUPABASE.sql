-- ============================================================
-- PREVALIDACIÓN DEL ESQUEMA REAL DE SUPABASE - EMPLANORTE
-- Este archivo NO modifica datos. Ejecútelo primero en Supabase.
-- Debe devolver cero filas en la consulta "faltantes".
-- ============================================================

WITH requeridos(tabla, columna) AS (
    VALUES
        ('usuarios','id'),
        ('usuarios','nombre'),
        ('usuarios','contrasena_hash'),
        ('productos','id'),
        ('productos','nombre'),
        ('productos','costo_unitario'),
        ('productos','stock_disponible'),
        ('productos','stock_minimo'),
        ('productos','activo'),
        ('ventas','id'),
        ('ventas','fecha_venta'),
        ('ventas','total'),
        ('ventas','total_costo'),
        ('ventas','estado'),
        ('gastos','id'),
        ('gastos','fecha_gasto'),
        ('gastos','valor')
), existentes AS (
    SELECT table_name, column_name
    FROM information_schema.columns
    WHERE table_schema = 'public'
)
SELECT r.tabla, r.columna, 'FALTA EN SUPABASE' AS resultado
FROM requeridos r
LEFT JOIN existentes e
  ON e.table_name = r.tabla AND e.column_name = r.columna
WHERE e.column_name IS NULL
ORDER BY r.tabla, r.columna;

-- Inventario informativo de las tablas que usará la migración.
SELECT table_name, column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_schema = 'public'
  AND table_name IN ('usuarios','productos','ventas','detalle_ventas','gastos')
ORDER BY table_name, ordinal_position;
