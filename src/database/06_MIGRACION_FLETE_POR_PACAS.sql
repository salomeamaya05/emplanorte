-- ============================================================
-- MIGRACIÓN ADITIVA - DISTRIBUCIÓN DEL FLETE POR PACAS/BULTOS
-- EMPLANORTE / PostgreSQL - Supabase
-- Conserva las compras históricas y habilita el nuevo cálculo.
-- ============================================================

BEGIN;

DO $$
DECLARE
    faltante TEXT;
BEGIN
    SELECT string_agg(tabla || '.' || columna, ', ')
      INTO faltante
      FROM (
        SELECT r.tabla, r.columna
          FROM (VALUES
            ('productos','id'),('productos','costo_unitario'),('productos','stock_disponible'),
            ('compras','id'),('compras','subtotal'),('compras','flete'),
            ('detalle_compras','id'),('detalle_compras','id_compra'),('detalle_compras','cantidad'),('detalle_compras','subtotal_linea')
          ) AS r(tabla,columna)
          LEFT JOIN information_schema.columns c
            ON c.table_schema='public' AND c.table_name=r.tabla AND c.column_name=r.columna
         WHERE c.column_name IS NULL
      ) x;

    IF faltante IS NOT NULL THEN
        RAISE EXCEPTION 'Migración detenida. Faltan objetos requeridos: %', faltante;
    END IF;
END $$;

ALTER TABLE productos
    ADD COLUMN IF NOT EXISTS unidades_por_paca INTEGER NOT NULL DEFAULT 1;

ALTER TABLE compras
    ADD COLUMN IF NOT EXISTS metodo_distribucion_flete VARCHAR(20) NOT NULL DEFAULT 'pacas';

ALTER TABLE detalle_compras
    ADD COLUMN IF NOT EXISTS cantidad_pacas INTEGER,
    ADD COLUMN IF NOT EXISTS unidades_por_paca INTEGER,
    ADD COLUMN IF NOT EXISTS flete_asignado NUMERIC(14,2),
    ADD COLUMN IF NOT EXISTS flete_unitario NUMERIC(14,4);

-- Las compras antiguas usaron reparto por valor. Se identifican para no
-- presentarlas como si hubieran sido calculadas con el método nuevo.
UPDATE compras
   SET metodo_distribucion_flete='valor'
 WHERE metodo_distribucion_flete='pacas'
   AND EXISTS (SELECT 1 FROM detalle_compras d WHERE d.id_compra=compras.id)
   AND NOT EXISTS (
       SELECT 1 FROM detalle_compras d
        WHERE d.id_compra=compras.id AND d.cantidad_pacas IS NOT NULL
   );

-- Para el historial se conserva una paca equivalente por línea. El flete
-- histórico se reconstruye con el reparto proporcional que usaba el sistema.
UPDATE detalle_compras d
   SET cantidad_pacas=COALESCE(d.cantidad_pacas,1),
       unidades_por_paca=COALESCE(d.unidades_por_paca,d.cantidad),
       flete_asignado=COALESCE(
           d.flete_asignado,
           CASE WHEN c.subtotal>0
                THEN ROUND(d.subtotal_linea*c.flete/c.subtotal,2)
                ELSE 0 END
       )
  FROM compras c
 WHERE c.id=d.id_compra;

UPDATE detalle_compras
   SET flete_unitario=COALESCE(flete_unitario,ROUND(flete_asignado/cantidad,4));

ALTER TABLE detalle_compras
    ALTER COLUMN cantidad_pacas SET NOT NULL,
    ALTER COLUMN unidades_por_paca SET NOT NULL,
    ALTER COLUMN flete_asignado SET NOT NULL,
    ALTER COLUMN flete_unitario SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
         WHERE conname='ck_productos_unidades_por_paca_positivas'
           AND conrelid='productos'::regclass
    ) THEN
        ALTER TABLE productos
            ADD CONSTRAINT ck_productos_unidades_por_paca_positivas
            CHECK (unidades_por_paca>0);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
         WHERE conname='ck_compras_metodo_distribucion_flete'
           AND conrelid='compras'::regclass
    ) THEN
        ALTER TABLE compras
            ADD CONSTRAINT ck_compras_metodo_distribucion_flete
            CHECK (metodo_distribucion_flete IN ('pacas','valor'));
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
         WHERE conname='ck_detalle_compras_cantidad_pacas_positiva'
           AND conrelid='detalle_compras'::regclass
    ) THEN
        ALTER TABLE detalle_compras
            ADD CONSTRAINT ck_detalle_compras_cantidad_pacas_positiva
            CHECK (cantidad_pacas>0);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
         WHERE conname='ck_detalle_compras_unidades_paca_positivas'
           AND conrelid='detalle_compras'::regclass
    ) THEN
        ALTER TABLE detalle_compras
            ADD CONSTRAINT ck_detalle_compras_unidades_paca_positivas
            CHECK (unidades_por_paca>0);
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
         WHERE conname='ck_detalle_compras_flete_no_negativo'
           AND conrelid='detalle_compras'::regclass
    ) THEN
        ALTER TABLE detalle_compras
            ADD CONSTRAINT ck_detalle_compras_flete_no_negativo
            CHECK (flete_asignado>=0 AND flete_unitario>=0);
    END IF;
END $$;

COMMENT ON COLUMN productos.unidades_por_paca IS 'Presentación habitual usada para precargar compras; puede cambiarse en cada recepción';
COMMENT ON COLUMN compras.metodo_distribucion_flete IS 'Método aplicado al registrar la compra: pacas para compras nuevas, valor para historial anterior';
COMMENT ON COLUMN detalle_compras.cantidad_pacas IS 'Número de pacas o bultos recibidos en la línea';
COMMENT ON COLUMN detalle_compras.unidades_por_paca IS 'Unidades contenidas en cada paca al momento de la compra';
COMMENT ON COLUMN detalle_compras.flete_asignado IS 'Parte del flete total asignada a la línea según sus pacas';
COMMENT ON COLUMN detalle_compras.flete_unitario IS 'Flete asignado por unidad comprada';

DO $$
DECLARE
    faltante TEXT;
BEGIN
    SELECT string_agg(tabla || '.' || columna, ', ')
      INTO faltante
      FROM (
        SELECT r.tabla, r.columna
          FROM (VALUES
            ('productos','unidades_por_paca'),
            ('compras','metodo_distribucion_flete'),
            ('detalle_compras','cantidad_pacas'),
            ('detalle_compras','unidades_por_paca'),
            ('detalle_compras','flete_asignado'),
            ('detalle_compras','flete_unitario')
          ) AS r(tabla,columna)
          LEFT JOIN information_schema.columns c
            ON c.table_schema='public' AND c.table_name=r.tabla AND c.column_name=r.columna
         WHERE c.column_name IS NULL
      ) x;

    IF faltante IS NOT NULL THEN
        RAISE EXCEPTION 'Migración incompleta. Faltan columnas: %', faltante;
    END IF;
END $$;

COMMIT;
