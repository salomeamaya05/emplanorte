-- Verificación no destructiva de la migración v2.3.0.

-- A. Deben aparecer las cinco columnas nuevas de ventas.
SELECT column_name, data_type, is_nullable, column_default
FROM information_schema.columns
WHERE table_schema = current_schema()
  AND table_name = 'ventas'
  AND column_name IN (
      'editada', 'fecha_ultima_edicion', 'id_venta_origen',
      'id_venta_reemplazo', 'motivo_anulacion'
  )
ORDER BY column_name;

-- B. Deben aparecer las tres columnas nuevas de auditoría.
SELECT column_name, data_type, is_nullable
FROM information_schema.columns
WHERE table_schema = current_schema()
  AND table_name = 'auditoria_ventas'
  AND column_name IN ('motivo', 'detalle_cambios', 'id_venta_relacionada')
ORDER BY column_name;

-- C. Revisar restricciones e índices creados.
SELECT conname, pg_get_constraintdef(oid) AS definicion
FROM pg_constraint
WHERE conrelid IN ('ventas'::regclass, 'auditoria_ventas'::regclass)
  AND conname IN (
      'fk_ventas_origen',
      'fk_ventas_reemplazo',
      'fk_auditoria_ventas_relacionada',
      'auditoria_ventas_accion_check'
  )
ORDER BY conname;

SELECT indexname, indexdef
FROM pg_indexes
WHERE schemaname = current_schema()
  AND indexname IN (
      'uq_ventas_id_venta_origen',
      'uq_ventas_id_venta_reemplazo',
      'idx_auditoria_ventas_fecha',
      'idx_ventas_fecha_registro'
  )
ORDER BY indexname;

-- D. Conteos de seguridad. Esta consulta no modifica datos.
SELECT
    COUNT(*) AS total_ventas,
    COUNT(*) FILTER (WHERE editada) AS ventas_editadas,
    COUNT(*) FILTER (WHERE id_venta_origen IS NOT NULL) AS ventas_correccion,
    COUNT(*) FILTER (WHERE id_venta_reemplazo IS NOT NULL) AS ventas_reemplazadas
FROM ventas;
