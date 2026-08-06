-- ============================================================
-- EMPLANORTE v2.3.0
-- Edición administrativa, fecha real y corrección trazable de ventas
-- Migración ADITIVA: no borra ventas, detalles, inventario ni auditorías.
-- Ejecutar una sola vez en Supabase antes de desplegar el backend v2.3.0.
-- ============================================================

BEGIN;

-- 1. Datos de control en la venta.
ALTER TABLE ventas
    ADD COLUMN IF NOT EXISTS editada BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS fecha_ultima_edicion TIMESTAMP,
    ADD COLUMN IF NOT EXISTS id_venta_origen INT,
    ADD COLUMN IF NOT EXISTS id_venta_reemplazo INT,
    ADD COLUMN IF NOT EXISTS motivo_anulacion TEXT;

-- 2. Relaciones entre una venta anulada y la nueva venta que la corrige.
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_ventas_origen'
          AND conrelid = 'ventas'::regclass
    ) THEN
        ALTER TABLE ventas
            ADD CONSTRAINT fk_ventas_origen
            FOREIGN KEY (id_venta_origen) REFERENCES ventas(id) ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_ventas_reemplazo'
          AND conrelid = 'ventas'::regclass
    ) THEN
        ALTER TABLE ventas
            ADD CONSTRAINT fk_ventas_reemplazo
            FOREIGN KEY (id_venta_reemplazo) REFERENCES ventas(id) ON DELETE SET NULL;
    END IF;
END $$;

-- Una venta anulada solo puede tener una corrección y una venta corregida
-- solo puede tener un origen.
CREATE UNIQUE INDEX IF NOT EXISTS uq_ventas_id_venta_origen
    ON ventas(id_venta_origen) WHERE id_venta_origen IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS uq_ventas_id_venta_reemplazo
    ON ventas(id_venta_reemplazo) WHERE id_venta_reemplazo IS NOT NULL;

-- 3. Ampliar la bitácora para guardar motivo y cambios antes/después.
ALTER TABLE auditoria_ventas
    ADD COLUMN IF NOT EXISTS motivo TEXT,
    ADD COLUMN IF NOT EXISTS detalle_cambios TEXT,
    ADD COLUMN IF NOT EXISTS id_venta_relacionada INT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'fk_auditoria_ventas_relacionada'
          AND conrelid = 'auditoria_ventas'::regclass
    ) THEN
        ALTER TABLE auditoria_ventas
            ADD CONSTRAINT fk_auditoria_ventas_relacionada
            FOREIGN KEY (id_venta_relacionada) REFERENCES ventas(id) ON DELETE SET NULL;
    END IF;
END $$;

-- 4. Reemplazar de forma segura el CHECK antiguo de accion.
DO $$
DECLARE
    restriccion RECORD;
BEGIN
    FOR restriccion IN
        SELECT c.conname
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        JOIN pg_namespace n ON n.oid = t.relnamespace
        WHERE n.nspname = current_schema()
          AND t.relname = 'auditoria_ventas'
          AND c.contype = 'c'
          AND pg_get_constraintdef(c.oid) ILIKE '%accion%'
    LOOP
        EXECUTE format(
            'ALTER TABLE auditoria_ventas DROP CONSTRAINT %I',
            restriccion.conname
        );
    END LOOP;

    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'auditoria_ventas_accion_check'
          AND conrelid = 'auditoria_ventas'::regclass
    ) THEN
        ALTER TABLE auditoria_ventas
            ADD CONSTRAINT auditoria_ventas_accion_check
            CHECK (accion IN (
                'creacion',
                'edicion',
                'anulacion',
                'anulacion_correccion',
                'correccion_creada'
            ));
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_auditoria_ventas_fecha
    ON auditoria_ventas(id_venta, fecha_registro);
CREATE INDEX IF NOT EXISTS idx_ventas_fecha_registro
    ON ventas(creado_en);

COMMENT ON COLUMN ventas.editada IS
    'TRUE cuando se corrigieron cliente, fecha, metodo de pago u observaciones.';
COMMENT ON COLUMN ventas.id_venta_origen IS
    'Venta anulada que dio origen a esta venta corregida.';
COMMENT ON COLUMN ventas.id_venta_reemplazo IS
    'Nueva venta que reemplaza esta venta anulada.';
COMMENT ON COLUMN ventas.motivo_anulacion IS
    'Motivo obligatorio de la anulacion o correccion.';
COMMENT ON COLUMN auditoria_ventas.detalle_cambios IS
    'Resumen inmutable de valores anteriores y nuevos.';

COMMIT;
