-- ============================================================
-- MIGRACIÓN ADITIVA - PROVEEDORES, COMPRAS, FACTURAS Y PAGOS
-- EMPLANORTE / PostgreSQL - Supabase
-- No elimina ni reemplaza ventas, gastos, clientes o inventario.
-- ============================================================

BEGIN;

-- Validación defensiva contra el esquema REAL de Supabase.
DO $$
DECLARE
    faltante TEXT;
BEGIN
    SELECT string_agg(tabla || '.' || columna, ', ')
    INTO faltante
    FROM (
        SELECT r.tabla, r.columna
        FROM (VALUES
            ('usuarios','id'), ('usuarios','nombre'), ('usuarios','contrasena_hash'),
            ('productos','id'), ('productos','nombre'), ('productos','costo_unitario'),
            ('productos','stock_disponible'), ('productos','stock_minimo'), ('productos','activo'),
            ('ventas','id'), ('ventas','fecha_venta'), ('ventas','total'),
            ('ventas','total_costo'), ('ventas','estado'),
            ('gastos','id'), ('gastos','fecha_gasto'), ('gastos','valor')
        ) AS r(tabla,columna)
        LEFT JOIN information_schema.columns c
          ON c.table_schema='public' AND c.table_name=r.tabla AND c.column_name=r.columna
        WHERE c.column_name IS NULL
    ) x;

    IF faltante IS NOT NULL THEN
        RAISE EXCEPTION 'Migración detenida. Faltan objetos requeridos en Supabase: %', faltante;
    END IF;
END $$;

ALTER TABLE productos
    ADD COLUMN IF NOT EXISTS unidades_por_paca INTEGER NOT NULL DEFAULT 1;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname='ck_productos_unidades_por_paca_positivas'
          AND conrelid='productos'::regclass
    ) THEN
        ALTER TABLE productos
            ADD CONSTRAINT ck_productos_unidades_por_paca_positivas
            CHECK (unidades_por_paca > 0);
    END IF;
END $$;

CREATE TABLE IF NOT EXISTS proveedores (
    id                  BIGSERIAL       PRIMARY KEY,
    nit_documento       VARCHAR(50)     NOT NULL UNIQUE,
    razon_social        VARCHAR(150)    NOT NULL,
    contacto_nombre     VARCHAR(120),
    telefono            VARCHAR(30),
    correo              VARCHAR(150),
    direccion           TEXT,
    ciudad              VARCHAR(100),
    condiciones_pago    VARCHAR(100)    NOT NULL DEFAULT 'Contado',
    observaciones       TEXT,
    activo              BOOLEAN         NOT NULL DEFAULT TRUE,
    creado_en           TIMESTAMP       NOT NULL DEFAULT NOW(),
    actualizado_en      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS compras (
    id                  BIGSERIAL       PRIMARY KEY,
    numero_compra       VARCHAR(30)     NOT NULL UNIQUE,
    id_proveedor        BIGINT          NOT NULL REFERENCES proveedores(id),
    id_usuario          INTEGER         NOT NULL REFERENCES usuarios(id),
    fecha_compra        TIMESTAMP       NOT NULL DEFAULT NOW(),
    subtotal            NUMERIC(14,2)   NOT NULL DEFAULT 0 CHECK (subtotal >= 0),
    flete               NUMERIC(14,2)   NOT NULL DEFAULT 0 CHECK (flete >= 0),
    impuestos           NUMERIC(14,2)   NOT NULL DEFAULT 0 CHECK (impuestos >= 0),
    descuento           NUMERIC(14,2)   NOT NULL DEFAULT 0 CHECK (descuento >= 0),
    total               NUMERIC(14,2)   NOT NULL DEFAULT 0 CHECK (total >= 0),
    metodo_distribucion_flete VARCHAR(20) NOT NULL DEFAULT 'pacas'
                                        CONSTRAINT ck_compras_metodo_distribucion_flete
                                        CHECK (metodo_distribucion_flete IN ('pacas','valor')),
    estado              VARCHAR(20)     NOT NULL DEFAULT 'registrada'
                                        CHECK (estado IN ('registrada','anulada')),
    observaciones       TEXT,
    motivo_anulacion    TEXT,
    creado_en           TIMESTAMP       NOT NULL DEFAULT NOW(),
    anulado_en          TIMESTAMP
);

CREATE TABLE IF NOT EXISTS detalle_compras (
    id                          BIGSERIAL       PRIMARY KEY,
    id_compra                   BIGINT          NOT NULL REFERENCES compras(id) ON DELETE CASCADE,
    id_producto                 INTEGER         NOT NULL REFERENCES productos(id),
    cantidad                    INTEGER         NOT NULL CHECK (cantidad > 0),
    cantidad_pacas              INTEGER         NOT NULL
                                                CONSTRAINT ck_detalle_compras_cantidad_pacas_positiva CHECK (cantidad_pacas > 0),
    unidades_por_paca           INTEGER         NOT NULL
                                                CONSTRAINT ck_detalle_compras_unidades_paca_positivas CHECK (unidades_por_paca > 0),
    costo_unitario              NUMERIC(12,2)   NOT NULL CHECK (costo_unitario >= 0),
    costo_unitario_inventario   NUMERIC(12,2)   NOT NULL CHECK (costo_unitario_inventario >= 0),
    subtotal_linea              NUMERIC(14,2)   NOT NULL CHECK (subtotal_linea >= 0),
    flete_asignado              NUMERIC(14,2)   NOT NULL,
    flete_unitario              NUMERIC(14,4)   NOT NULL,
    stock_anterior              INTEGER         NOT NULL CHECK (stock_anterior >= 0),
    costo_anterior              NUMERIC(12,2)   NOT NULL CHECK (costo_anterior >= 0),
    stock_posterior             INTEGER         NOT NULL CHECK (stock_posterior >= 0),
    costo_promedio_posterior    NUMERIC(12,2)   NOT NULL CHECK (costo_promedio_posterior >= 0),
    CONSTRAINT ck_detalle_compras_flete_no_negativo CHECK (flete_asignado >= 0 AND flete_unitario >= 0)
);

CREATE TABLE IF NOT EXISTS facturas_proveedores (
    id                  BIGSERIAL       PRIMARY KEY,
    id_compra           BIGINT          NOT NULL UNIQUE REFERENCES compras(id),
    id_proveedor        BIGINT          NOT NULL REFERENCES proveedores(id),
    numero_factura      VARCHAR(80)     NOT NULL,
    fecha_emision       DATE            NOT NULL DEFAULT CURRENT_DATE,
    fecha_vencimiento   DATE,
    total_factura       NUMERIC(14,2)   NOT NULL CHECK (total_factura >= 0),
    total_pagado        NUMERIC(14,2)   NOT NULL DEFAULT 0 CHECK (total_pagado >= 0),
    saldo_pendiente     NUMERIC(14,2)   NOT NULL CHECK (saldo_pendiente >= 0),
    estado_pago         VARCHAR(20)     NOT NULL DEFAULT 'pendiente'
                                        CHECK (estado_pago IN ('pendiente','parcial','pagada','anulada')),
    ruta_adjunto        TEXT,
    nombre_adjunto      VARCHAR(255),
    tipo_adjunto        VARCHAR(100),
    observaciones       TEXT,
    creado_en           TIMESTAMP       NOT NULL DEFAULT NOW(),
    actualizado_en      TIMESTAMP       NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_factura_proveedor_numero UNIQUE (id_proveedor, numero_factura)
);

CREATE TABLE IF NOT EXISTS pagos_proveedores (
    id                  BIGSERIAL       PRIMARY KEY,
    id_factura          BIGINT          NOT NULL REFERENCES facturas_proveedores(id),
    id_usuario          INTEGER         NOT NULL REFERENCES usuarios(id),
    fecha_pago          TIMESTAMP       NOT NULL DEFAULT NOW(),
    monto               NUMERIC(14,2)   NOT NULL CHECK (monto > 0),
    metodo_pago         VARCHAR(30)     NOT NULL
                                        CHECK (metodo_pago IN ('efectivo','transferencia','tarjeta','otro')),
    referencia          VARCHAR(150),
    observaciones       TEXT,
    estado              VARCHAR(20)     NOT NULL DEFAULT 'activo'
                                        CHECK (estado IN ('activo','anulado')),
    motivo_anulacion    TEXT,
    anulado_en          TIMESTAMP,
    creado_en           TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS auditoria_compras (
    id                  BIGSERIAL       PRIMARY KEY,
    id_compra           BIGINT          NOT NULL REFERENCES compras(id),
    id_usuario          INTEGER         REFERENCES usuarios(id),
    usuario_nombre      VARCHAR(150),
    accion              VARCHAR(30)     NOT NULL
                                        CHECK (accion IN ('creacion','anulacion')),
    numero_compra       VARCHAR(30),
    total               NUMERIC(14,2),
    estado              VARCHAR(20),
    motivo              TEXT,
    fecha_registro      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS auditoria_pagos_proveedores (
    id                  BIGSERIAL       PRIMARY KEY,
    id_pago             BIGINT          NOT NULL REFERENCES pagos_proveedores(id),
    id_factura          BIGINT          NOT NULL REFERENCES facturas_proveedores(id),
    id_usuario          INTEGER         REFERENCES usuarios(id),
    usuario_nombre      VARCHAR(150),
    accion              VARCHAR(30)     NOT NULL
                                        CHECK (accion IN ('creacion','anulacion')),
    monto               NUMERIC(14,2),
    motivo              TEXT,
    fecha_registro      TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_proveedores_razon_social ON proveedores(razon_social);
CREATE UNIQUE INDEX IF NOT EXISTS uq_proveedores_nit_ci ON proveedores(LOWER(nit_documento));
CREATE INDEX IF NOT EXISTS idx_compras_fecha ON compras(fecha_compra DESC);
CREATE INDEX IF NOT EXISTS idx_compras_proveedor ON compras(id_proveedor);
CREATE INDEX IF NOT EXISTS idx_detalle_compras_compra ON detalle_compras(id_compra);
CREATE INDEX IF NOT EXISTS idx_detalle_compras_producto ON detalle_compras(id_producto);
CREATE INDEX IF NOT EXISTS idx_facturas_estado_vencimiento ON facturas_proveedores(estado_pago, fecha_vencimiento);
CREATE UNIQUE INDEX IF NOT EXISTS uq_factura_proveedor_numero_ci ON facturas_proveedores(id_proveedor, LOWER(numero_factura));
CREATE INDEX IF NOT EXISTS idx_facturas_proveedor ON facturas_proveedores(id_proveedor);
CREATE INDEX IF NOT EXISTS idx_pagos_factura ON pagos_proveedores(id_factura, estado);

CREATE SEQUENCE IF NOT EXISTS seq_numero_compra START WITH 1 INCREMENT BY 1;

DO $$
DECLARE
    maximo BIGINT;
BEGIN
    SELECT COALESCE(MAX((regexp_match(numero_compra, '([0-9]+)$'))[1]::BIGINT), 0)
      INTO maximo
      FROM compras
     WHERE numero_compra ~ '[0-9]+$';

    IF maximo = 0 THEN
        PERFORM setval('seq_numero_compra', 1, FALSE);
    ELSE
        PERFORM setval('seq_numero_compra', maximo, TRUE);
    END IF;
END $$;

CREATE OR REPLACE FUNCTION fn_generar_numero_compra()
RETURNS TEXT
LANGUAGE plpgsql
AS $$
BEGIN
    RETURN 'CMP-' || LPAD(nextval('seq_numero_compra')::TEXT, 6, '0');
END;
$$;

COMMENT ON TABLE proveedores IS 'Proveedores de mercancía de EMPLANORTE';
COMMENT ON TABLE compras IS 'Compras de inventario; no se mezclan con gastos operativos';
COMMENT ON TABLE facturas_proveedores IS 'Facturas recibidas y cuentas por pagar';
COMMENT ON TABLE pagos_proveedores IS 'Abonos y pagos inmutables a proveedores';

-- Verificación transaccional: detecta tablas preexistentes incompletas antes de confirmar.
DO $$
DECLARE
    faltante TEXT;
BEGIN
    SELECT string_agg(tabla || '.' || columna, ', ')
      INTO faltante
      FROM (
        SELECT r.tabla, r.columna
        FROM (VALUES
          ('proveedores','id'),('proveedores','nit_documento'),('proveedores','razon_social'),('proveedores','activo'),
          ('compras','id'),('compras','numero_compra'),('compras','id_proveedor'),('compras','id_usuario'),('compras','total'),('compras','metodo_distribucion_flete'),('compras','estado'),
          ('detalle_compras','id'),('detalle_compras','id_compra'),('detalle_compras','id_producto'),('detalle_compras','cantidad'),('detalle_compras','cantidad_pacas'),('detalle_compras','unidades_por_paca'),('detalle_compras','flete_asignado'),('detalle_compras','flete_unitario'),('detalle_compras','costo_unitario_inventario'),('detalle_compras','stock_anterior'),('detalle_compras','stock_posterior'),('detalle_compras','costo_promedio_posterior'),
          ('facturas_proveedores','id'),('facturas_proveedores','id_compra'),('facturas_proveedores','numero_factura'),('facturas_proveedores','total_factura'),('facturas_proveedores','saldo_pendiente'),('facturas_proveedores','estado_pago'),('facturas_proveedores','ruta_adjunto'),
          ('pagos_proveedores','id'),('pagos_proveedores','id_factura'),('pagos_proveedores','monto'),('pagos_proveedores','estado'),
          ('auditoria_compras','id'),('auditoria_compras','id_compra'),('auditoria_compras','accion'),
          ('auditoria_pagos_proveedores','id'),('auditoria_pagos_proveedores','id_pago'),('auditoria_pagos_proveedores','accion')
        ) AS r(tabla,columna)
        LEFT JOIN information_schema.columns c
          ON c.table_schema='public' AND c.table_name=r.tabla AND c.column_name=r.columna
        WHERE c.column_name IS NULL
      ) x;

    IF faltante IS NOT NULL THEN
        RAISE EXCEPTION 'Migración incompleta: faltan columnas nuevas: %', faltante;
    END IF;
END $$;

COMMIT;
